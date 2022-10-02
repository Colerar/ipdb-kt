package moe.sdl.ipdb.exceptions

public class NoSuchLanguageException internal constructor(
    language: String
) : IllegalStateException("No such language: $language")
