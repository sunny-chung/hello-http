package com.sunnychung.application.multiplatform.hellohttp

import com.sunnychung.application.multiplatform.hellohttp.manager.FileManager
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistResponseManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistenceManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PrettifierManager
import com.sunnychung.application.multiplatform.hellohttp.repository.ProjectCollectionRepository
import com.sunnychung.application.multiplatform.hellohttp.repository.RequestCollectionRepository
import com.sunnychung.application.multiplatform.hellohttp.repository.ResponseCollectionRepository
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.DialogViewModel
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.EditRequestNameViewModel

object AppContext {
    val NetworkManager = NetworkManager()
    val FileManager = FileManager()
    val PersistenceManager = PersistenceManager()
    val PrettifierManager = PrettifierManager()
    val PersistResponseManager = PersistResponseManager()

    val RequestCollectionRepository = RequestCollectionRepository()
    val ProjectCollectionRepository = ProjectCollectionRepository()
    val ResponseCollectionRepository = ResponseCollectionRepository()

    val DialogViewModel = DialogViewModel()
//    val EditRequestNameViewModel = EditRequestNameViewModel()
}
