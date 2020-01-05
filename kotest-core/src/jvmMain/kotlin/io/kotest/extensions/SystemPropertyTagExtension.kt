package io.kotest.extensions

import io.kotest.core.tags.StringTag
import io.kotest.core.tags.Tag
import io.kotest.core.tags.Tags

/**
 * This [TagExtension] includes and excludes tags using the system properties
 * 'kotest.tags.include' and 'kotest.tags.exclude'
 */
object SystemPropertyTagExtension : TagExtension {

  override fun tags(): Tags {

    fun readTagsProperty(name: String): List<Tag> =
      (System.getProperty(name) ?: "").split(',').filter { it.isNotBlank() }.map {
          StringTag(
              it.trim()
          )
      }

    val includedTags = readTagsProperty("kotest.tags.include")
    val excludedTags = readTagsProperty("kotest.tags.exclude")

    return Tags(includedTags.toSet(), excludedTags.toSet())
  }
}
