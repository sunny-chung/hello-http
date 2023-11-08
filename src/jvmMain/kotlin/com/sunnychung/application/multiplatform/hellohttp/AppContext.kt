package com.sunnychung.application.multiplatform.hellohttp

import com.sunnychung.application.multiplatform.hellohttp.manager.ApacheNetworkManager
import com.sunnychung.application.multiplatform.hellohttp.manager.AutoBackupManager
import com.sunnychung.application.multiplatform.hellohttp.manager.FileManager
import com.sunnychung.application.multiplatform.hellohttp.manager.MetadataManager
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkManager
import com.sunnychung.application.multiplatform.hellohttp.manager.OkHttpNetworkManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistResponseManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistenceManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PrettifierManager
import com.sunnychung.application.multiplatform.hellohttp.manager.SingleInstanceProcessService
import com.sunnychung.application.multiplatform.hellohttp.repository.ProjectCollectionRepository
import com.sunnychung.application.multiplatform.hellohttp.repository.RequestCollectionRepository
import com.sunnychung.application.multiplatform.hellohttp.repository.ResponseCollectionRepository
import com.sunnychung.application.multiplatform.hellohttp.repository.UserPreferenceRepository
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.DialogViewModel
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.ResponseViewModel
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.UserPreferenceViewModel

object AppContext {
    val MetadataManager = MetadataManager()
    val SingleInstanceProcessService = SingleInstanceProcessService()
    val NetworkManager: NetworkManager = ApacheNetworkManager() //OkHttpNetworkManager()
    val FileManager = FileManager()
    val PersistenceManager = PersistenceManager()
    val PrettifierManager = PrettifierManager()
    val PersistResponseManager = PersistResponseManager()
    val AutoBackupManager = AutoBackupManager()

    val RequestCollectionRepository = RequestCollectionRepository()
    val ProjectCollectionRepository = ProjectCollectionRepository()
    val ResponseCollectionRepository = ResponseCollectionRepository()
    val UserPreferenceRepository = UserPreferenceRepository()

    val DialogViewModel = DialogViewModel()
//    val EditRequestNameViewModel = EditRequestNameViewModel()
    val UserPreferenceViewModel = UserPreferenceViewModel()
    val ResponseViewModel = ResponseViewModel()
}
