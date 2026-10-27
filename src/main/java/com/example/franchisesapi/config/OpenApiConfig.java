package com.example.franchisesapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Metadatos de la documentación OpenAPI (Swagger UI en /swagger-ui.html) y respuestas
 * de error reutilizables: los controladores las referencian con las constantes *_RESPONSE
 * en lugar de repetir su definición en cada endpoint.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String FRANCHISES_TAG = "Franquicias";
    public static final String BRANCHES_TAG = "Sucursales";
    public static final String PRODUCTS_TAG = "Productos";

    public static final String BAD_REQUEST_RESPONSE = "#/components/responses/BadRequest";
    public static final String NOT_FOUND_RESPONSE = "#/components/responses/NotFound";
    public static final String CONFLICT_RESPONSE = "#/components/responses/Conflict";
    public static final String UNPROCESSABLE_RESPONSE = "#/components/responses/UnprocessableContent";

    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    OpenAPI franchisesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Franchises API")
                        .version("v1")
                        .description("""
                                API REST para gestionar franquicias, sus sucursales y los productos \
                                ofertados en cada sucursal, con su stock.

                                Todas las respuestas de error siguen el formato **Problem Details** \
                                (RFC 9457) con `Content-Type: application/problem+json`.""")
                        .contact(new Contact()
                                .name("Repositorio en GitHub")
                                .url("https://github.com/EliecerUT/franchise-management-api")))
                .addTagsItem(new Tag().name(FRANCHISES_TAG)
                        .description("Alta y consulta de franquicias y reporte de stock por sucursal"))
                .addTagsItem(new Tag().name(BRANCHES_TAG)
                        .description("Sucursales de una franquicia"))
                .addTagsItem(new Tag().name(PRODUCTS_TAG)
                        .description("Productos de una sucursal y su stock"))
                .components(new Components()
                        .addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema())
                        .addResponses("BadRequest", problemResponse(
                                "Petición inválida: algún campo no cumple las validaciones o el JSON está mal formado"))
                        .addResponses("NotFound", problemResponse(
                                "La franquicia, la sucursal o el producto no existe"))
                        .addResponses("Conflict", problemResponse(
                                "Ya existe un recurso con el mismo nombre o código, o la franquicia fue "
                                        + "modificada por otra petición al mismo tiempo"))
                        .addResponses("UnprocessableContent", problemResponse(
                                "La petición es válida pero el estado actual no permite la operación")));
    }

    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("Error con formato Problem Details (RFC 9457)")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Not Found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema().example("Franchise '6ae0375cc698e94971e2fce3' not found"))
                .addProperty("instance", new StringSchema().example("/api/v1/franchises/6ae0375cc698e94971e2fce3"))
                .addProperty("errors", new ObjectSchema()
                        .description("Solo en errores de validación (400): campo -> motivo")
                        .example(Map.of("name", "must not be blank")));
    }

    private static ApiResponse problemResponse(String description) {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL_SCHEMA);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON, new MediaType().schema(schema)));
    }

}
