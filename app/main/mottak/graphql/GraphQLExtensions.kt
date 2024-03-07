package mottak.graphql

internal data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

internal class GraphQLWarning(
    val query:String?,
    val id:String?,
    val code: String?,
    val message: String?,
    val details: String?,
)
