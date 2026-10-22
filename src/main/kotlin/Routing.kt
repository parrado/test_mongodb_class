package com.example

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class UserRequest(
    @SerialName("userName") val userName: String,
    @SerialName("password") val password: String
)


@Serializable
data class User (
    @SerialName("_id") var id: String = ObjectId.get().toString(),
    var userName: String? = null,
    var session: String? = null,
    var password: String? = null
)

val mongoJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun User.toDocument(): Document {
    val jsonString = mongoJson.encodeToString(this)
    return Document.parse(jsonString)
}

fun Document.toObject(): User {
    val jsonString = this.toJson()
    return mongoJson.decodeFromString<User>(jsonString)
}

fun Application.configureRouting() {

    val client = MongoClients.create("mongodb://localhost:27017")
    val database = client.getDatabase("smartlock_db_nohash")
    val usersCollection: MongoCollection<Document> = database.getCollection("users")

    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }

        post("/register") {
            val request = call.receive<UserRequest>()

            // Query using raw Documents
            val existingUserDoc = usersCollection.find(Filters.eq("userName", request.userName)).first()
            if (existingUserDoc != null) {
                // Convert to Kotlin class if a match is found
                val existingUser = existingUserDoc.toObject()
                println("User ${existingUser.userName} already exists.")
                return@post call.respond(HttpStatusCode.OK, mapOf("status" to "User exists"))
            }

            val newUser = User(
                userName = request.userName,
                password = request.password
            )

            // Convert User object to BSON Document and insert
            usersCollection.insertOne(newUser.toDocument())
            println("$newUser registered successfully")

            call.respond(HttpStatusCode.OK, mapOf("status" to "User registered"))
        }
    }
}