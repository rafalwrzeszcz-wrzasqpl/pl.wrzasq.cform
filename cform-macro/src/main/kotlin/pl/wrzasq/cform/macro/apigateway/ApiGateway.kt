/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.popProperty

private val SLUG_FILTER = Regex("\\W")

/**
 * API Gateway definition.
 *
 * @param id Resource logical ID.
 * @param input Resource specification.
 */
class ApiGateway(
    val id: String,
    input: Map<String, Any>
) : ApiTemplateResource {
    private val properties: Map<String, Any>

    val authorizers: MutableMap<String, ApiAuthorizer> = mutableMapOf()
    private val models: MutableMap<String, ApiModel> = mutableMapOf()
    private val validators: MutableMap<String, ApiRequestValidator> = mutableMapOf()
    private val resources: MutableMap<String, ApiResource> = mutableMapOf()
    private val methods: MutableMap<String, ApiMethod> = mutableMapOf()
    private val resourceIds: MutableSet<String> = mutableSetOf()

    private lateinit var deploymentHash: String

    init {
        properties = input
            .popProperty("Authorizers", {
                for ((authorizerId, definition) in asMap(it)) {
                    authorizers[authorizerId] = ApiAuthorizer(this, authorizerId, asMap(definition))
                }
            })
            .popProperty("Models", {
                for ((modelId, definition) in asMap(it)) {
                    models[modelId] = ApiModel(this, modelId, asMap(definition))
                }
            })
            .popProperty("Resources", {
                initTree(Fn.getAtt(resourceId, "RootResourceId"), asMap(it))
            })
    }

    override val resourceId = "ApiGateway$id"

    /**
     * Returns validator of given type.
     *
     * @param validatorType Type of validation.
     * @return Validator resource.
     */
    fun getValidator(validatorType: String) = validators.computeIfAbsent(validatorType) {
        when (it) {
            "BODY_ONLY" -> ApiRequestValidator(this, "BodyOnly", true, false)
            "PARAMETERS_ONLY" -> ApiRequestValidator(this, "ParametersOnly", false, true)
            "BODY_AND_PARAMETERS" -> ApiRequestValidator(this, "BodyAndParameters", true, true)
            else -> throw IllegalArgumentException("Unsupported validator type `$it`")
        }
    }

    /**
     * Builds resources definitions.
     *
     * @return List of resource entries.
     */
    fun generateResources(): List<ResourceDefinition> {
        val definitions = mutableListOf<ResourceDefinition>()

        // api resource
        definitions.add(
            ResourceDefinition(
                id = resourceId,
                type = "AWS::ApiGateway::RestApi",
                properties = properties
            )
        )

        // sorted resources to ensure consistent order for hash computation
        definitions.addAll(authorizers.toSortedMap().values.map(ApiAuthorizer::generateResource))
        definitions.addAll(models.toSortedMap().values.map(ApiModel::generateResource))
        definitions.addAll(validators.toSortedMap().values.map(ApiRequestValidator::generateResource))
        definitions.addAll(resources.toSortedMap().values.map(ApiResource::generateResource))
        definitions.addAll(methods.toSortedMap().values.map(ApiMethod::generateResource))

        // deployment
        definitions.add(
            ResourceDefinition(
                id = "${resourceId}Deployment${computeDeploymentHash(definitions)}",
                type = "AWS::ApiGateway::Deployment",
                dependsOn = methods.values.map(ApiMethod::resourceId).sorted(),
                properties = mapOf("RestApiId" to ref())
            )
        )

        return definitions
    }

    private fun computeDeploymentHash(definitions: List<ResourceDefinition>): String {
        deploymentHash = definitions.hashCode().toString().replace("-", "_")
        return deploymentHash
    }

    private fun initTree(
        parent: Any,
        input: Map<String, Any>,
        scopePath: String = "",
        scopeId: String = ""
    ) {
        for ((key, value) in input) {
            when (key[0]) {
                // sub-resource
                '/' -> {
                    val part = key.substring(1)
                    val path = "$scopePath$key"
                    val id = generateResourceId(scopeId, part)

                    val resource = ApiResource(this, id, parent, part)
                    resources[path] = resource
                    resourceIds.add(id)

                    // https://knowyourmeme.com/memes/we-need-to-go-deeper
                    initTree(resource.ref(), asMap(value), path, id)
                }
                // method
                '@' -> {
                    val method = key.substring(1)
                    methods["$method$scopePath"] = ApiMethod(this, "$method$scopeId", parent, method, asMap(value))
                }
                else -> throw IllegalArgumentException("`$key` is not a valid API structure entry")
            }
        }
    }

    private fun generateResourceId(scope: String, part: String): String {
        val slug = SLUG_FILTER.replace(part, "").replaceFirstChar { it.uppercase() }
        var id = "$scope$slug"

        while (id in resourceIds) {
            id += "_"
        }

        return id
    }

    fun resolve(path: List<String>): String {
        if (path.isEmpty()) {
            // root element reference
            return resourceId
        } else if (path.first() == "Deployment") {
            // resolving takes place after resources construction so hash will be available
            return "${resourceId}Deployment${deploymentHash}"
        }

        val lookupId = path[1]

        return when (path.first()) {
            "Authorizer" -> authorizers[lookupId] ?: throw IllegalArgumentException("Unknown authorizer $lookupId")
            "Validator" -> validators[lookupId] ?: throw IllegalArgumentException("Unknown validator $lookupId")
            "Model" -> models[lookupId] ?: throw IllegalArgumentException("Unknown model $lookupId")
            "Resource" -> {
                val key = unescapeReference(lookupId)
                resources[key] ?: throw IllegalArgumentException("Unknown resource $key")
            }
            "Method" -> {
                val key = unescapeReference(lookupId)
                methods[key] ?: throw IllegalArgumentException("Unknown resource method $key")
            }
            else -> throw IllegalArgumentException("Unknown resource type reference ${path.first()}")
        }.resourceId
    }
}

// since in API gateway only full path parts are allowed as fragments this is only pattern possibility
private fun unescapeReference(reference: String) = reference.replace("/%", "/{").replace("%/", "}/")
