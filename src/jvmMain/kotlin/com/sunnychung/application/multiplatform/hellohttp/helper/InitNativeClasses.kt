package com.sunnychung.application.multiplatform.hellohttp.helper

import io.github.dralletje.ktreesitter.graphql.TreeSitterGraphql
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Tree
import io.github.treesitter.ktreesitter.json.TreeSitterJson

class InitNativeClasses {

    init {
        Parser(Language(TreeSitterJson.language())).also {
            val t: Tree = it.parse("")
        }
        Language(TreeSitterGraphql.language())
    }
}
