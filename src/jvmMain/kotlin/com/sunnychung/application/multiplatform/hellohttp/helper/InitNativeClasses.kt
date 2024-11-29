package com.sunnychung.application.multiplatform.hellohttp.helper

import io.github.dralletje.ktreesitter.graphql.TreeSitterGraphql
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Tree

class InitNativeClasses {

    init {
        Parser(Language(TreeSitterGraphql.language())).also {
            val t: Tree = it.parse("")
        }
    }
}
