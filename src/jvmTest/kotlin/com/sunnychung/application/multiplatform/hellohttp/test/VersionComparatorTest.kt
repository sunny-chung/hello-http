package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.model.Version
import kotlin.test.Test

class VersionComparatorTest {

    @Test
    fun identical() {
        assert(Version("1.23.456") == Version("1.23.456"))
    }

    @Test
    fun `same num of dots but different versions`() {
        assert(Version("1.4.5") > Version("1.4.3"))
        assert(Version("1.4.5") >= Version("1.4.3"))
        assert(Version("1.4.3") < Version("1.4.5"))
        assert(Version("1.4.3") <= Version("1.4.5"))

        assert(Version("1.6.0") > Version("1.5.1"))
        assert(Version("1.6.0") >= Version("1.5.1"))
        assert(Version("1.5.1") < Version("1.6.0"))
        assert(Version("1.5.1") <= Version("1.6.0"))

        assert(Version("1.23.4") > Version("1.2.3"))
        assert(Version("1.23.4") >= Version("1.2.3"))
        assert(Version("1.2.3") < Version("1.23.4"))
        assert(Version("1.2.3") <= Version("1.23.4"))
    }

    @Test
    fun `same num of dots with text`() {
        assert(Version("1.5.0-SNAPSHOT") == Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5.0-SNAPSHOT") >= Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5.0-SNAPSHOT") <= Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5.0") > Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5.0") >= Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5") > Version("1.5-SNAPSHOT"))
        assert(Version("1.5") >= Version("1.5-SNAPSHOT"))
        assert(Version("1.5.1") > Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5.1") >= Version("1.5.0-SNAPSHOT"))
        assert(Version("1.6.0") > Version("1.5.0-SNAPSHOT"))
        assert(Version("1.6.0") >= Version("1.5.0-SNAPSHOT"))
        assert(Version("1.4.99") < Version("1.5.0-SNAPSHOT"))
        assert(Version("1.4.99") <= Version("1.5.0-SNAPSHOT"))
    }

    @Test
    fun `different num of dots`() {
        assert(Version("1.5") > Version("1.4.3"))
        assert(Version("1.5") >= Version("1.4.3"))

        assert(Version("1.5") < Version("1.5.1"))
        assert(Version("1.5") <= Version("1.5.1"))

        assert(Version("1.5") == Version("1.5.0"))
        assert(Version("1.5") <= Version("1.5.0"))
        assert(Version("1.5") >= Version("1.5.0"))

        assert(Version("1.5.0.1") > Version("1.5.0"))
        assert(Version("1.5.0.1") >= Version("1.5.0"))
    }

    @Test
    fun `different num of dots with text`() {
        assert(Version("1.5") > Version("1.5.0-SNAPSHOT"))
        assert(Version("1.5") >= Version("1.5.0-SNAPSHOT"))

        assert(Version("1.4") < Version("1.5.0-SNAPSHOT"))
        assert(Version("1.4") <= Version("1.5.0-SNAPSHOT"))

        assert(Version("1.5.1") > Version("1.5-SNAPSHOT"))
        assert(Version("1.5.1") >= Version("1.5-SNAPSHOT"))

        assert(Version("1.4.9") < Version("1.5-SNAPSHOT"))
        assert(Version("1.4.9") <= Version("1.5-SNAPSHOT"))
    }
}
