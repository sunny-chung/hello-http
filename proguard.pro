# fix the bug: java.lang.VerifyError: Bad type on operand stack
-dontoptimize

-dontwarn jakarta.**
-dontwarn jboss.**
-dontwarn org.jboss.**
-dontwarn org.eclipse.jetty.**
-dontwarn reactor.blockhound.**
-dontwarn org.conscrypt.**
-dontwarn org.aspectj.**
-dontwarn io.micrometer.**
-dontwarn reactor.netty.http.server.**
-dontwarn reactor.netty.http.observability.**
#-dontwarn org.springframework.boot.**
#-dontwarn org.springframework.web.reactive.socket.**
#-dontwarn org.springframework.web.reactive.result.view.**
#-dontwarn org.springframework.web.reactive.resource.WebJarsResourceResolver
#-dontwarn org.springframework.http.server.**
#-dontwarn org.springframework.http.codec.protobuf.**
#-dontwarn org.springframework.http.codec.xml.**
#-dontwarn org.springframework.format.number.money.**
#-dontwarn org.springframework.web.filter.**
#-dontwarn org.springframework.web.context.support.**
#-dontwarn org.springframework.validation.beanvalidation.**
-dontwarn org.springframework.**
-dontwarn groovy.**
-dontwarn bsh.**
-dontwarn org.codehaus.**
-dontwarn android.**
-dontwarn com.jayway.jsonpath.spi.**
-dontwarn com.dslplatform.json.jsonb.**
-dontwarn com.dslplatform.json.runtime.**
-dontwarn java.lang.invoke.MethodHandle
-dontwarn reactor.netty.channel.ChannelMeters
-dontwarn org.brotli.**
-dontwarn com.oracle.svm.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.**
-dontwarn okhttp3.**
-dontwarn sun.**

# netty-tcnative is not used
-dontwarn io.netty.internal.tcnative.**

# not sure
-dontwarn reactor.netty.resources.**
-dontwarn reactor.netty.transport.**
-dontwarn reactor.netty.http.**
-dontwarn io.grpc.netty.shaded.io.netty.handler.codec.**
-dontwarn io.netty.handler.codec.**

# fix the bug: java.lang.SecurityException: SHA-256 digest error for org/bouncycastle/asn1/ASN1Primitive.class
-keep class kotlinx.coroutines.internal.LimitedDispatcherKt {
    static boolean checkParallelism(int);
}
-keep class kotlinx.coroutines.AbstractTimeSourceKt

# fix the bug: java.util.ServiceConfigurationError: kotlinx.coroutines.internal.MainDispatcherFactory: Provider kotlinx.coroutines.swing.SwingDispatcherFactory not found
# https://github.com/Kotlin/kotlinx.coroutines/issues/4025
# https://github.com/JetBrains/compose-multiplatform/issues/4288
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory

-keep class kotlinx.coroutines.reactor.ReactorContextInjector

# fix: java.lang.NullPointerException: Cannot read the array length because "<local2>" is null
-keep enum * implements com.fasterxml.jackson.databind.cfg.ConfigFeature { *; }

# JNI stuffs
-keep class io.github.treesitter.ktreesitter.* { *; }

-keep class org.apache.logging.log4j.LogManager
-keep enum org.apache.logging.log4j.spi.StandardLevel { *; }
-keep class org.springframework.http.codec.support.DefaultClientCodecConfigurer

# needed to keep CodecConfigurerFactory not to crash during init
-keep class org.springframework.http.codec.support.DefaultServerCodecConfigurer

-keep enum org.springframework.util.ConcurrentReferenceHashMap$TaskOption { *; }
-keep class com.fasterxml.jackson.module.kotlin.KotlinModule {
    <init>(...);
}

# fix: class com.jayway.jsonpath.Option not an enum
-keep enum com.jayway.jsonpath.Option

# fix: io.grpc.ManagedChannelProvider: Provider io.grpc.netty.shaded.io.grpc.netty.NettyChannelProvider not found
# fix: io.grpc.ManagedChannelProvider: Provider io.grpc.netty.shaded.io.grpc.netty.UdsNettyChannelProvider not found
-keep class * extends io.grpc.ManagedChannelProvider

# fix: io.grpc.NameResolverProvider: Provider io.grpc.netty.shaded.io.grpc.netty.UdsNameResolverProvider not found
-keep class * extends io.grpc.NameResolverProvider

# fix: Could not initialize class io.grpc.netty.shaded.io.grpc.netty.ProtocolNegotiators
# Caused by: java.lang.ExceptionInInitializerError: Exception java.lang.ClassCastException: class io.grpc.TlsChannelCredentials$Feature not an enum
# and java.lang.ClassCastException: class io.grpc.TlsChannelCredentials$Feature not an enum
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum io.grpc.TlsChannelCredentials$Feature {
    *;
    static *;
}
#-keep class io.grpc.TlsChannelCredentials { *; }
#-keep class java.lang.Enum { *; }

# fix: java.util.ServiceConfigurationError: io.grpc.LoadBalancerProvider: Provider io.grpc.protobuf.services.internal.HealthCheckingRoundRobinLoadBalancerProvider not found
# fix: io.grpc.LoadBalancerProvider: Provider io.grpc.util.OutlierDetectionLoadBalancerProvider not found
-keep class * extends io.grpc.LoadBalancerProvider
