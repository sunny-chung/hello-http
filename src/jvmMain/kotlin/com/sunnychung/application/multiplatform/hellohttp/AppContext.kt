package com.sunnychung.application.multiplatform.hellohttp

import com.sunnychung.application.multiplatform.hellohttp.network.ApacheHttpTransportClient
import com.sunnychung.application.multiplatform.hellohttp.manager.AutoBackupManager
import com.sunnychung.application.multiplatform.hellohttp.manager.FileManager
import com.sunnychung.application.multiplatform.hellohttp.network.GraphqlSubscriptionTransportClient
import com.sunnychung.application.multiplatform.hellohttp.manager.MetadataManager
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.network.TransportClient
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistResponseManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PersistenceManager
import com.sunnychung.application.multiplatform.hellohttp.manager.PrettifierManager
import com.sunnychung.application.multiplatform.hellohttp.manager.SingleInstanceProcessService
import com.sunnychung.application.multiplatform.hellohttp.network.GrpcTransportClient
import com.sunnychung.application.multiplatform.hellohttp.network.WebSocketTransportClient
import com.sunnychung.application.multiplatform.hellohttp.repository.ApiSpecificationCollectionRepository
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
    val NetworkClientManager = NetworkClientManager()
    val HttpTransportClient: TransportClient = ApacheHttpTransportClient(NetworkClientManager) //OkHttpNetworkManager(NetworkClientManager)
    val WebSocketTransportClient: TransportClient = WebSocketTransportClient(NetworkClientManager)
    val GraphqlSubscriptionTransportClient = GraphqlSubscriptionTransportClient(NetworkClientManager)
    val GrpcTransportClient = GrpcTransportClient(NetworkClientManager)
    val FileManager = FileManager()
    val PersistenceManager = PersistenceManager()
    val PrettifierManager = PrettifierManager()
    val PersistResponseManager = PersistResponseManager()
    val AutoBackupManager = AutoBackupManager()

    val RequestCollectionRepository = RequestCollectionRepository()
    val ProjectCollectionRepository = ProjectCollectionRepository()
    val ResponseCollectionRepository = ResponseCollectionRepository()
    val ApiSpecificationCollectionRepository = ApiSpecificationCollectionRepository()
    val UserPreferenceRepository = UserPreferenceRepository()

    val DialogViewModel = DialogViewModel()
//    val EditRequestNameViewModel = EditRequestNameViewModel()
    val UserPreferenceViewModel = UserPreferenceViewModel()
    val ResponseViewModel = ResponseViewModel()
}
