package pl.kvgx12.fbchat.session

import io.ktor.resources.*

@Resource("/")
internal class Messenger {
    @Resource("login/")
    class Login(val parent: Messenger = Messenger()) {
        @Resource("password/")
        class Password(val parent: Login = Login())

        @Resource("auth_token/")
        class AuthToken(val parent: Login = Login())
    }

    @Resource("logout/")
    class Logout(val parent: Messenger = Messenger())

    @Resource("checkpoint/")
    class Checkpoint(val parent: Messenger = Messenger()) {
        @Resource("block/")
        class Block(val parent: Checkpoint = Checkpoint())

        @Resource("start/")
        class Start(val parent: Checkpoint = Checkpoint())
    }

    @Resource("api/")
    class Api(val parent: Messenger = Messenger()) {
        @Resource("graphqlbatch/")
        class GraphQLBatch(val parent: Api = Api())

        @Resource("graphql/")
        class GraphQL(val parent: Api = Api())
    }

    @Resource("webgraphql/")
    class WebGraphQL(val parent: Messenger = Messenger()) {
        @Resource("mutation")
        class Mutation(val parent: WebGraphQL = WebGraphQL())
    }

    @Resource("messaging/")
    class Messaging(val parent: Messenger = Messenger()) {
        @Resource("send/")
        class Send(val parent: Messaging = Messaging())
    }
}
