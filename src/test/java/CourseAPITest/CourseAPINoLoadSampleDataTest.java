package CourseAPITest;

import static CourseAPITest.CourseAPITest.MODULE_FROM;
import static CourseAPITest.CourseAPITest.MODULE_TO;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CourseAPINoLoadSampleDataTest extends CourseAPIWithSampleDataTest {

  
  @BeforeClass
  public static void beforeClass(TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    okapiPort = NetworkUtils.nextFreePort();
    baseUrl = "http://localhost:"+port+"/coursereserves";
    okapiUrl = "http://localhost:"+okapiPort;
    //okapiUrl = "http://localhost:" + port;
    okapiTenantUrl = "http://localhost:" + port;
    //TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");
    okapiHeaders.put("x-okapi-tenant", "diku");
    okapiHeaders.put("x-okapi-url", okapiUrl);
    standardHeaders.add("x-okapi-url", okapiUrl);
    acceptTextHeaders.add("accept", "text/plain");
    acceptTextHeaders.add("x-okapi-url", okapiUrl);
    vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port));
    DeploymentOptions okapiOptions = new DeploymentOptions()
        .setConfig(new JsonObject().put("port", okapiPort));
    try {
      PostgresClient.setEmbeddedPort(NetworkUtils.nextFreePort());
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
      return;
    }
    vertx.deployVerticle(OkapiMock.class.getName(), okapiOptions, deployOkapiRes -> {
      if(deployOkapiRes.failed()) {
        context.fail(deployOkapiRes.cause());
      } else {
        okapiVerticleId = deployOkapiRes.result();
        logger.info("Deployed Mock Okapi on port " + okapiPort);
        vertx.deployVerticle(RestVerticle.class.getName(), options, deployCourseRes -> {
          if(deployCourseRes.failed()) {
            context.fail(deployCourseRes.cause());
          } else {
            resetMockOkapi().compose(f -> {
              return addSampleData();
            }).setHandler(res -> {
              try {
                restVerticleId = deployCourseRes.result();
                logger.info("Deployed verticle on port " + port);
                initTenant("diku", port).setHandler(initRes -> {
                  if(initRes.failed()) {
                    context.fail(initRes.cause());
                  } else {
                    async.complete();
                  }
                });
              } catch(Exception e) {
                e.printStackTrace();
                context.fail(e);
              }
            });
          }
        });
      }
    });
  }
  

  @AfterClass
  public static void afterClass(TestContext context) {
    Async async = context.async();
    vertx.undeploy(okapiVerticleId, undeployOkapiRes -> {
      if(undeployOkapiRes.failed()) {
        context.fail(undeployOkapiRes.cause());
      } else {
        vertx.undeploy(restVerticleId, undeployCourseRes -> {
          if(undeployCourseRes.failed()) {
            context.fail(undeployCourseRes.cause());
          } else {
            PostgresClient.stopEmbeddedPostgres();
            async.complete();
            /*
            vertx.close(context.asyncAssertSuccess( res -> {
              PostgresClient.stopEmbeddedPostgres();
              try {
                Thread.sleep(3000);
              } catch(Exception e) {
                logger.error(e.getLocalizedMessage());
              }
              async.complete();
            }));
            */
          }
        });
      }
    });
  }


  @Before
  @Override
  public void beforeEach(TestContext context) {
    Async async = context.async();
    resetMockOkapi().setHandler(res -> {
      addSampleData().setHandler(res2 -> {
        if(res2.failed()) {
          context.fail(res2.cause());
        } else {
          async.complete();
        }
      });
    });
  }

  @After
  @Override
  public void afterEach(TestContext context) {
    Async async = context.async();
    logger.info("After each");
    async.complete();
  }

  

  protected static Future<Void> initTenant(String tenantId, int port) {
    Promise<Void> promise = Promise.promise();
    HttpClient client = vertx.createHttpClient();
    //String url = "http://localhost:" + port + "/_/tenant?tenantParameters=loadSample=false";
    String url = "http://localhost:" + port + "/_/tenant";
    JsonObject payload = new JsonObject()
        .put("module_to", MODULE_TO)
        .put("module_from", MODULE_FROM)
        .put("parameters", new JsonArray()
          .add(new JsonObject()
            .put("key", "loadSample")
            .put("value", false))
         );
    HttpClientRequest request = client.postAbs(url);
    request.handler(req -> {
      if(req.statusCode() != 201) {
        promise.fail("Expected 201, got " + req.statusCode());
      } else {
        promise.complete();
      }
    });
    request.putHeader("X-Okapi-Tenant", tenantId);
    request.putHeader("X-Okapi-Url", okapiUrl);
    request.putHeader("X-Okapi-Url-To", okapiTenantUrl);
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Accept", "application/json, text/plain");
    request.end(payload.encode());
    return promise.future();
  }

  //Tests

  @Test
  @Override
  public void testCourseListingLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/cef52efb-b3fd-4450-9960-1745026a99d1",
        GET, standardHeaders, null, 404, "Get CourseListing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  @Override
  public void testReserveLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/reserves/67227d94-7333-4d22-98a0-718b49d36595",
        GET, standardHeaders, null, 404, "Get Reserve").onComplete(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

}


