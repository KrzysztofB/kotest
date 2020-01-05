package io.kotest.provided

import io.kotest.core.AbstractProjectConfig
import io.kotest.spring.SpringAutowireConstructorExtension

class ProjectConfig : AbstractProjectConfig() {
  override fun extensions() = listOf(SpringAutowireConstructorExtension)
}
