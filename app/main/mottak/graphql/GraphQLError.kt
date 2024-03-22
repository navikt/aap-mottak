package mottak.graphql

import org.intellij.lang.annotations.Language

data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>,
    val path: List<String>?,
    val extensions: GraphQLErrorExtension
)

@Language("JSON")
val tes = """
   {
  "errors": [
    {
      "message": "Validation error (FieldUndefined@[journalpost/journalposter]) : Field 'journalposter' in type 'Journalpost' is undefined",
      "locations": [
        {
          "line": 1,
          "column": 83
        }
      ],
      "extensions": {
        "classification": "ValidationError"
      }
    }
  ]
}
 
""".trimIndent()

data class GraphQLErrorExtension(
    val code: String?,
    val classification: String
)

data class GraphQLErrorLocation(
    val line: Int?,
    val column: Int?
)
