package br.edu.fatecsbc.sigapi.doc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.restassured3.operation.preprocess.RestAssuredPreprocessors.modifyUris;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@SpringBootTest(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class DocTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    private RequestSpecification spec;

    @Before
    public void setUp() {
        spec = new RequestSpecBuilder() //
            .addFilter( //
                documentationConfiguration(restDocumentation)) //
            .build(); //
    }

    private void basicAssert(final String documentName, final ResponseFieldsSnippet fields) {

        final Response response = createResponse(documentName, documentName, null, fields);
        response.then().assertThat().statusCode(is(200));

    }

    private void basicAssert(final String documentName, final String path, final PathParametersSnippet parameters,
        final ResponseFieldsSnippet fields, final Object... values) {

        final Response response = createResponse(documentName, path, parameters, fields, values);
        response.then().assertThat().statusCode(is(200));

    }

    private Response createResponse(final String documentName, final String path,
        final PathParametersSnippet parameters, final ResponseFieldsSnippet fields, final Object... values) {

        final OperationRequestPreprocessor preprocessRequest = preprocessRequest( //
            modifyUris().host("api.sigapi.info").removePort(), //
            removeHeaders("Accept") //
        );

        final OperationResponsePreprocessor preprocessResponse = preprocessResponse( //
            prettyPrint(), //
            removeHeaders("Content-Encoding", "Vary", "Transfer-Encoding", "Server", "Content-Length") //
        );

        final List<Snippet> snippetsList = Arrays.asList(parameters, fields);
        final Snippet[] snippetsArray = snippetsList.stream().filter(Objects::nonNull).toArray(Snippet[]::new);

        return given(spec).accept(ContentType.JSON) //
            .filter(document(documentName, preprocessRequest, preprocessResponse, snippetsArray)) //
            .when().port(8089) //
            .header(HttpHeaders.AUTHORIZATION, //
                "Bearer ${TOKEN}" //
            ) //
            .get("/api/" + path, values);
    }

    @Test
    public void dadosCadastrais() throws Exception {

        final FieldDescriptor[] dadosCadastrais = new FieldDescriptor[] {
            fieldWithPath("nome").description("Nome do aluno"), //
            fieldWithPath("foto").description("URL da foto do aluno"), //
            fieldWithPath("ra").description("Registro acadêmico do aluno"), //
            fieldWithPath("instituicao").description("Instituição matriculada"), //
            fieldWithPath("curso").description("Curso matriculado"), //
            fieldWithPath("turno").description("Turno matriculado"), //
            fieldWithPath("emailEtec").description("Email cadastrado na ETECT").optional().type("String"), //
            fieldWithPath("emailFatec").description("Email cadastrado na FATEC").optional(), //
            fieldWithPath("emailWebsai").description("Email cadastrado no WEBSAI").optional(), //
            fieldWithPath("emailPreferencial").description("Email cadastrado como preferencial"), //
            fieldWithPath("outrosEmails").description("Outros emails cadastrados").optional(), //
        };

        basicAssert("dados-cadastrais", responseFields(dadosCadastrais));
    }

    @Test
    public void dadosDesempenho() throws Exception {

        final FieldDescriptor[] dadosCadastrais = new FieldDescriptor[] {
            fieldWithPath("pp").description("Índice PP do aluno"), //
            fieldWithPath("pr").description("Índice PR do aluno"), //
            fieldWithPath("maiorPrCurso").description("Maior PR do curso") //
        };

        basicAssert("dados-desempenho", responseFields(dadosCadastrais));
    }

    @Test
    public void notasParciais() throws Exception {

        final ResponseFieldsSnippet descriptor = responseFields( //
            fieldWithPath("[]").description("Listagem de notas parciais"));

        basicAssert("notas-parciais", descriptor.andWithPrefix("[].", descriptorNotaParcial()));

    }

    @Test
    public void notasParciaisPorDisciplina() throws Exception {

        final PathParametersSnippet parameters = pathParameters( //
            parameterWithName("sigla").description("Sigla da disciplina desejada") //
        );

        basicAssert("notas-parciais_por-disciplina", "notas-parciais/{sigla}", parameters,
            responseFields(descriptorNotaParcial()), "AGO007");

    }

    @Test
    public void faltasParciais() throws Exception {

        final ResponseFieldsSnippet listaFaltasParciais = responseFields( //
            fieldWithPath("[]").description("Listagem de faltas parciais"));

        basicAssert("faltas-parciais", listaFaltasParciais.andWithPrefix("[].", descriptorFaltaParcial()));

    }

    @Test
    public void faltaParcialPorDisciplina() throws Exception {

        final PathParametersSnippet parameters = pathParameters( //
            parameterWithName("sigla").description("Sigla da disciplina desejada") //
        );

        basicAssert("faltas-parciais_por-disciplina", "faltas-parciais/{sigla}", parameters,
            responseFields(descriptorFaltaParcial()), "IMH002");

    }

    private static final FieldDescriptor[] descriptorNotaParcial() {

        final FieldDescriptor[] descriptor = new FieldDescriptor[] { //
            fieldWithPath("mediaFinal").description("Média final"), //
            fieldWithPath("quantidadeFaltas").description("Quantidade de faltas"), //
            fieldWithPath("percentualFrequencia").description("Percentual de frequência"), //
            fieldWithPath("avaliacoes").description("Avaliações"), //
            fieldWithPath("avaliacoes.*").description("Nota por avaliação"), //
        };

        return ArrayUtils.addAll(descriptor, descriptorDisciplina());

    }

    private static final FieldDescriptor[] descriptorFaltaParcial() {

        final FieldDescriptor[] descriptor = new FieldDescriptor[] { //
            fieldWithPath("quantidadeAusencias").description("Quantidade de ausências"), //
            fieldWithPath("quantidadePresencas").description("Quantidade de presenças") //
        };

        return ArrayUtils.addAll(descriptor, descriptorDisciplina());

    }

    private static final FieldDescriptor[] descriptorDisciplina() {

        return new FieldDescriptor[] { //
            fieldWithPath("siglaDisciplina").description("Sigla da disciplina"), //
            fieldWithPath("nomeDisciplina").description("Nome da disciplina"), //
        };

    }

}
