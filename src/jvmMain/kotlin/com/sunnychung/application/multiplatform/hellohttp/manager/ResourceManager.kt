package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.hello_http.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Why this class exists? Why can't I just use `Res.font.xxx`?
 *
 * Response: |
 *      Why the generated `Res` class does not contain the resources placed correctly inside the composeResources dir?
 *      I have spent a half day to investigate by changing Kotlin versions, Gradle versions, dependencies, source sets,
 *      and looking at release notes and tickets of Compose Multiplatform. Finally, I found that `Res.readBytes(path)`
 *      can actually find the resources but not the Compose Gradle plugin. Why???
 *      And why somebody else does not encounter the issue? Why my older project (ComposableTable's demo) works? Why I
 *      need to provide a 100% reproducible code to report an issue?
 *      I don't want to dig further to help JetBrains debugging their buggy products. Jetpack Compose is the source of
 *      frustrations and full of bugs. Not to mention how buggy their TextField and Compose Test are.
 *      But most of Kotlin developers think they are amazing. The Compose Test random blocking bugs I reported was
 *      finally reported by someone else after 9 months and got attentions. Looks like I am the only one to try out
 *      their new features and suffer. I have to document this frustration as a record for myself.
 *
 */
class ResourceManager {

    private val cache = ConcurrentHashMap<String, ByteArray>()

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadAllResources() {
        val resources = listOf(AppRes.Font.PitagonSansMonoRegular, AppRes.Font.PitagonSansMonoBold, AppRes.Font.CommeLight, AppRes.Font.CommeRegular, AppRes.Font.CommeSemiBold)
        withContext(Dispatchers.IO) {
            resources.forEach {
                launch {
                    cache[it.key] = Res.readBytes(it.path)
//                    cache[it.key] = javaClass.classLoader.getResourceAsStream(it.path).use { it.readBytes() }
                }
            }
        }
    }

    fun getResource(resource: AppRes.Resource) = cache[resource.key]!!
}

object AppRes {

    object Font {
        val PitagonSansMonoRegular = Resource("font:PitagonSansMonoRegular", "font/pitagon_sans_mono/PitagonSansMono-Regular.ttf")
        val PitagonSansMonoBold = Resource("font:PitagonSansMonoBold", "font/pitagon_sans_mono/PitagonSansMono-Bold.ttf")
        val CommeLight = Resource("font:CommeLight", "font/comme/Comme-Light.ttf")
        val CommeRegular = Resource("font:CommeRegular", "font/comme/Comme-Regular.ttf")
        val CommeSemiBold = Resource("font:CommeSemiBold", "font/comme/Comme-SemiBold.ttf")
    }

    data class Resource(val key: String, val path: String)
}
