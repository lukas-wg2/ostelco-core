package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/regions")
class RegionsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegions(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getRegions(identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @Path("/{countryCode}/kyc")
    fun kycResource(
            @NotNull
            @PathParam("countryCode")
            countryCode: String
    ): KycResource {
        return when (countryCode.toLowerCase()) {
            "sg" -> SingaporeKycResource(dao = dao)
            else -> KycResource(countryCode = countryCode, dao = dao)
        }
    }
}