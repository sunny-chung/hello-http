package com.sunnychung.application.multiplatform.hellohttp.repository

import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.Document
import com.sunnychung.application.multiplatform.hellohttp.document.DocumentIdentifier
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.util.logR
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kotlite.extension.fullClassName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.serialization.KSerializer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlin.random.Random

private val log = logR

sealed class BaseCollectionRepository<T : Document<ID>, ID : DocumentIdentifier>(private val serializer: KSerializer<T>) {

    private val persistenceManager by lazy { AppContext.PersistenceManager }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val name = javaClass.simpleName

    private val updates = ConcurrentLinkedQueue<ID>()
    private val timerFlow = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(2000)
            yield()
        }
    }

    private val updateManualTriggerChannel = Channel<Unit>()
    private var isExecutingUpdates = MutableStateFlow(-1)
    private val updating = AtomicInteger(0)

    private val publishUpdates = MutableSharedFlow<Pair<ID, String>>()
    private val publishNonPersistedUpdates = MutableSharedFlow<Pair<ID, String>>()

    init {
        timerFlow //.takeWhile { updates.isNotEmpty() }
            .combine(updateManualTriggerChannel.receiveAsFlow()) { _, _ -> }
            .onEach {
                if (updates.isNotEmpty()) {
                    log.d { "$name Request updates: ${updates.size}" }
                    val submittedUpdates = mutableSetOf<ID>()
                    while (updates.isNotEmpty()) {
                        val it = updates.poll()
                        if (submittedUpdates.contains(it)) continue
                        CoroutineScope(Dispatchers.IO).launch {
                            update(it) // update isExecutingUpdates inside this function
                        }
                        submittedUpdates.add(it)
                    }
                } else if (updating.get() <= 0) {
                    isExecutingUpdates.update { -2 }
                }
            }
            .launchIn(coroutineScope)

        runBlocking {
            // make sure updateManualTriggerChannel is not empty so that
            // the above flow can "combine" and run
            updateManualTriggerChannel.send(Unit)
        }
    }

    /**
     * To ensure only one I/O concurrent process per DocumentIdentifier.
     *
     * TODO: propagate exceptions back to UI
     */
    protected suspend fun <R> withLock(identifier: DocumentIdentifier, operation: suspend () -> R): R {
        if (log.config.minSeverity <= Severity.Verbose && identifier is ResponsesDI) {
            log.v(Throwable()) { "getLock $identifier" }
        } else {
            log.d { "getLock $identifier" }
        }
        return persistenceManager.documentLocks.getOrPut(identifier) { Mutex() }
            .also { log.d { "obtained lock $identifier $it" } }
            .withLock {
                log.d { "accquired lock $identifier" }
                operation()
                    .also { log.d { "release lock $identifier" } }
            }
    }

    protected abstract fun relativeFilePath(id: ID): String

    protected fun buildIndex(document: T) {}
    protected fun removeIndex(document: T) {}

    open suspend fun create(document: T) {
        val identifier = document.id
        withLock(identifier) {
            with(persistenceManager) {
                documentCaches[identifier] = document
                buildIndex(document)
                writeToFile(relativeFilePath(id = identifier), serializer, document)
            }
        }
    }

    open suspend fun read(identifier: ID): T? {
        return withLock(identifier) {
            log.d { "readWithoutLock $identifier" }
            readWithoutLock(identifier).also {
                log.d { "readWithoutLock $identifier done" }
            }
        }
    }

    open fun readCache(identifier: ID): T? {
        return persistenceManager.documentCaches[identifier] as T?
    }

    private suspend fun readWithoutLock(identifier: ID): T? {
        return with(persistenceManager) {
            val cache = documentCaches[identifier]
            if (cache != null) return cache as T
            val persisted: T = readFile(relativeFilePath(id = identifier), serializer) ?: return null
            documentCaches[identifier] = persisted
            buildIndex(persisted)
            persisted
        }
    }

    open suspend fun readOrCreate(identifier: ID, documentSupplier: (ID) -> T): T {
        return withLock(identifier) {
            val record = readWithoutLock(identifier)
            if (record != null) return@withLock record
            with(persistenceManager) {
                val document = documentSupplier(identifier)
                val identifier = document.id
                documentCaches[identifier] = document
                buildIndex(document)
                writeToFile(relativeFilePath(id = identifier), serializer, document)
                document
            }
        }
    }

    private suspend fun update(identifier: ID) {
        log.d { "update: $identifier" }
        updating.addAndGet(1)
        try {
            withLock(identifier) {
                with(persistenceManager) {
                    val document = documentCaches[identifier]
                        ?: throw IllegalStateException("Cache miss. This should not happen.")
                    writeToFile(relativeFilePath(id = identifier), serializer, document as T)
                }
            }
        } catch (e: Throwable) {
            log.w(e) { "Cannot update document ${relativeFilePath(identifier)}" }
            throw e
        } finally {
            if (updating.decrementAndGet() <= 0) {
                log.d { "finish update: $identifier" }
                isExecutingUpdates.update { -3 }
            }
        }
        publishUpdates.emit(Pair(identifier, uuidString()))
    }

    open fun notifyUpdated(identifier: ID) {
        updates += identifier
        CoroutineScope(Dispatchers.Main.immediate).launch {
            publishNonPersistedUpdates.emit(Pair(identifier, uuidString()))
        }
    }

    suspend fun awaitUpdate(identifier: ID) {
        notifyUpdated(identifier)
        update(identifier)
    }

    suspend fun awaitAllUpdates() {
        val x = Random.nextInt().absoluteValue
        log.d { "${javaClass.simpleName} awaitAllUpdates start $x" }
        isExecutingUpdates.update { x }
        updateManualTriggerChannel.send(Unit)
//        isExecutingUpdates.first { it }
        isExecutingUpdates
            .onEach {
                log.d { "isExecutingUpdates -> $it" }
            }.first { it < 0 }
        log.d { "${javaClass.simpleName} awaitAllUpdates end" }
    }

    open suspend fun delete(identifier: ID) {
        withLock(identifier) {
            with(persistenceManager) {
                val document = documentCaches.remove(identifier)
                (document as? T)?.let { removeIndex(it) }
                deleteFile(relativeFilePath(id = identifier))
            }
        }
    }

    open fun subscribeUpdates(): SharedFlow<Pair<ID, String>> = publishUpdates

    open fun subscribeUnfilteredUpdates(): SharedFlow<Pair<ID, String>> = publishNonPersistedUpdates

    open fun subscribeLatestCollection(di: ID): Flow<Pair<T?, String>> = publishNonPersistedUpdates
        .onSubscription {
            emit(Pair(di, "onSub"))
        }
        .filter { it.first == di }
        .map { read(di) to it.second }
}
