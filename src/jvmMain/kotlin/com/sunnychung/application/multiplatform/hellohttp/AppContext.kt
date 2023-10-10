package com.sunnychung.application.multiplatform.hellohttp

import com.sunnychung.application.multiplatform.hellohttp.manager.FileManager
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistenceManager
import com.sunnychung.application.multiplatform.hellohttp.repository.RequestCollectionRepository

object AppContext {
    val NetworkManager = NetworkManager()
    val FileManager = FileManager()
    val PersistenceManager = PersistenceManager()

    val RequestCollectionRepository = RequestCollectionRepository()
}
