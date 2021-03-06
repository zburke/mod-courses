package CourseAPITest;

import CourseAPITest.TestUtil.WrappedResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
//import io.vertx.ext.asyncsql.AsyncSQLClient;
//import io.vertx.ext.sql.SQLClient;
//import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.coursereserves.util.CRUtil;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.impl.CourseAPI;
import static org.folio.rest.impl.CourseAPI.RESERVES_TABLE;
import static org.folio.rest.impl.CourseAPI.getCQL;
import org.folio.rest.jaxrs.model.CopyrightStatusObject;
import org.folio.rest.jaxrs.model.CopyrightTracking;
import org.folio.rest.jaxrs.model.Reserve;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.CourseListing;
import org.folio.rest.jaxrs.model.Instructor;
import org.folio.rest.jaxrs.model.LocationObject;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.tools.utils.NetworkUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class CourseAPITest {
  static int port;
  static int okapiPort;
  private static Vertx vertx;
  private static final Logger logger = LoggerFactory.getLogger(CourseAPITest.class);
  public static String baseUrl;
  public static String okapiUrl;
  public final static String COURSE_LISTING_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_LISTING_2_ID = UUID.randomUUID().toString();
  public final static String COURSE_LISTING_3_ID = UUID.randomUUID().toString();
  public final static String TERM_1_ID = UUID.randomUUID().toString();
  public final static String TERM_2_ID = UUID.randomUUID().toString();
  public final static String COURSE_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_2_ID = UUID.randomUUID().toString();
  public final static String COURSE_3_ID = UUID.randomUUID().toString();
  public final static String COURSE_4_ID = UUID.randomUUID().toString();
  public final static String COURSE_5_ID = UUID.randomUUID().toString();
  public final static String DEPARTMENT_1_ID = UUID.randomUUID().toString();
  public final static String DEPARTMENT_2_ID = UUID.randomUUID().toString();
  public final static String COURSE_TYPE_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_TYPE_2_ID = UUID.randomUUID().toString();
  public final static String INSTRUCTOR_1_ID = UUID.randomUUID().toString();
  public final static String INSTRUCTOR_2_ID = UUID.randomUUID().toString();
  public final static String INSTRUCTOR_3_ID = UUID.randomUUID().toString();
  public final static String COPYRIGHT_STATUS_1_ID = UUID.randomUUID().toString();
  public final static String COPYRIGHT_STATUS_2_ID = UUID.randomUUID().toString();
  public final static String PROCESSING_STATUS_1_ID = UUID.randomUUID().toString();
  public final static String PROCESSING_STATUS_2_ID = UUID.randomUUID().toString();
  public final static String EXTERNAL_ID_1 = "0001";
  public final static String EXTERNAL_ID_2 = "0002";
  public final static String EXTERNAL_ID_3 = "0003";
  public static Map<String, String> okapiHeaders = new HashMap<>();
  public static MultiMap standardHeaders = MultiMap.caseInsensitiveMultiMap();
  public static MultiMap acceptTextHeaders = MultiMap.caseInsensitiveMultiMap();
  public static String MODULE_TO = "1.0.1";
  public static String MODULE_FROM = "1.0.0";
  private static String restVerticleId;
  private static String okapiVerticleId;


  @Rule
  public Timeout rule = Timeout.seconds(200);

  @BeforeClass
  public static void beforeClass(TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    okapiPort = NetworkUtils.nextFreePort();
    baseUrl = "http://localhost:"+port+"/coursereserves";
    okapiUrl = "http://localhost:"+okapiPort;
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
      Thread.sleep(3000);
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
      return;
    }
    vertx.deployVerticle(RestVerticle.class.getName(), options, deployCourseRes -> {
      if(deployCourseRes.failed()) {
        context.fail(deployCourseRes.cause());
      } else {
        restVerticleId = deployCourseRes.result();
        try {
          initTenant("diku", port).setHandler(initRes -> {
            if(initRes.failed()) {
              context.fail(initRes.cause());
            } else {
              vertx.deployVerticle(OkapiMock.class.getName(), okapiOptions,
                deployOkapiRes -> {
                if(deployOkapiRes.failed()) {
                  context.fail(deployOkapiRes.cause());
                } else {
                  okapiVerticleId = deployOkapiRes.result();
                  async.complete();
                }
              });
            }
          });
        } catch(Exception e) {
          e.printStackTrace();
          context.fail(e);
        }
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
            try {
                PostgresClient.stopEmbeddedPostgres();
              } catch(Exception e) {
                logger.error(e.getLocalizedMessage());
              }
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
  public void beforeEach(TestContext context) {
    Async async = context.async();
    loadTerm1()
        .compose(f -> {
        return loadTerm2();
      })
      .compose(f -> {
        return loadCourseListing1();
      })
      .compose(f -> {
        return loadCourseListing2();
      })
      .compose(f -> {
        return loadCourseListing3();
      })
      .compose(f -> {
        return loadDepartment1();
      })
      .compose(f -> {
        return loadDepartment2();
      })
       .compose(f -> {
        return loadCourseListing1Instructor1();
      })
        .compose(f -> {
        return loadCourseListing1Instructor2();
      })
      .compose(f -> {
        return loadCourseListing2Instructor3();
      })
      .compose(f -> {
        return loadCourseType1();
      })
      .compose(f -> {
        return loadCourse1();
      })
      .compose(f -> {
        return loadCourse4();
      })
      .compose(f -> {
        return loadCourse2();
      })
      .compose(f -> {
        return loadCourse3();
      })
      .compose(f -> {
        return loadCourse5();
      })
      .compose(f -> {
        return loadProcessingStatus1();
      })
      .compose(f -> {
        return loadProcessingStatus2();
      })
      .compose(f -> {
        return loadCopyrightStatus1();
      })
      .compose(f -> {
        return loadCopyrightStatus2();
      })
      .setHandler(res -> {
        if(res.failed()) {
          context.fail(res.cause());
        } else {
          async.complete();
        }
      });
  }

  @After
  public void afterEach(TestContext context) {
    Async async = context.async();
    deleteCourses()
        .compose(f -> {
          return deleteCourseListing1Instructors();
        })
        .compose(f -> {
          return deleteCourseListing2Instructors();
        })
        .compose(f -> {
          return deleteReserves();
        })
        .compose(f -> {
          return deleteCourseListings();
        })
        .compose(f -> {
          return deleteTerms();
        }).compose(f -> {
          return deleteDepartments();
        }).compose(f -> {
          return deleteCourseTypes();
        }).compose(f -> {
          return deleteCopyrightStatuses();
        }).compose(f -> {
          return deleteProcessingStatuses();
        }).compose(f -> {
          return resetMockOkapi();
        }).setHandler(res -> {
        if(res.failed()) {
          context.fail(res.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void dummyTest(TestContext context) {
    Async async = context.async();
    async.complete();
  }

  @Test
  public void testOkapiReset(TestContext context) {
    Async async = context.async();
    JsonObject payload = new JsonObject().put("reset", true);
    try {
      TestUtil.doOkapiRequest(vertx, "/reset", POST, okapiHeaders, null,
          payload.encode(), 201, "Test Reset Okapi").onComplete(res -> {
        if(res.failed()) {
          res.cause().printStackTrace();
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          res.cause().printStackTrace(pw);
          String errmess = res.cause().getLocalizedMessage() + sw.toString();
          logger.error(errmess);
          context.fail(errmess);
        } else {
          async.complete();
        }
      });
    } catch(Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      context.fail("Error calling doOkapiRequest: " + e.getLocalizedMessage() + sw.toString());
    }
  }

  @Test
  public void testOkapiWipe(TestContext context) {
    Async async = context.async();
    JsonObject payload = new JsonObject().put("wipe", true);
    try {
      TestUtil.doOkapiRequest(vertx, "/wipe", POST, okapiHeaders, null,
          payload.encode(), 201, "Test Wipe Okapi").onComplete(res -> {
        if(res.failed()) {
          res.cause().printStackTrace();
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          res.cause().printStackTrace(pw);
          String errmess = res.cause().getLocalizedMessage() + sw.toString();
          logger.error(errmess);
          context.fail(errmess);
        } else {
          async.complete();
        }
      });
    } catch(Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      context.fail("Error calling doOkapiRequest: " + e.getLocalizedMessage() + sw.toString());
    }
  }

  @Test
  public void getRoles(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/roles", GET, null, null, 200,
        "Get role listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getCourseTypes(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/coursetypes", GET, null, null, 200,
        "Get course types listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getProcessingStatuses(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/processingstatuses", GET, null, null, 200,
        "Get processing status listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getCourses(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courses", GET, null, null, 200,
        "Get listing of courses").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson().getJsonArray("courses").getJsonObject(0);
          if(course.getJsonObject("courseListingObject") == null) {
            context.fail("No course listing object found");
            return;
          }
          if(course.getJsonObject("courseListingObject").getJsonObject("termObject") == null) {
            context.fail("No term object found in " + course.encode());
            return;
          }
          if(course.getJsonObject("departmentObject") == null) {
            context.fail("No department found in " + course.encode());
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getAllCoursesByQuery(TestContext context) {
     Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl +
        "/courses?query=cql.allRecords=1%20sortby%20name&limit=500", GET, null,
        null, 200, "Get courses by query").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson().getJsonArray("courses").getJsonObject(0);
          if(course.getJsonObject("courseListingObject") == null) {
            context.fail("No course listing object found");
            return;
          }
          if(res.result().getJson().getInteger("totalRecords") < 2) {
            context.fail("Expected at least two results");
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

 @Test
  public void getReserves(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/reserves", GET, null, null, 200,
        "Get reserve listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getInstructorsForCourseListing1(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/"
        + COURSE_LISTING_1_ID + "/instructors", GET, null, null, 200,
        "Get instructors for courselisting 1").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          if(res.result().getJson().getInteger("totalRecords") < 2) {
            context.fail("Expected at least two instructors");
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getCoursesByDepartment(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courses?query=departmentId==" + DEPARTMENT_1_ID,
        GET, null, null, 200, "Get courses for department").setHandler(res -> {
      if(res.failed()) { context.fail(res.cause()); }
      else {
        try {
          context.assertEquals(res.result().getJson().getJsonArray("courses").size(), 3);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getCourseById(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courses/" + COURSE_1_ID, GET, null, null, 200,
        "Get course by id").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson();
          JsonObject courseListing = course.getJsonObject("courseListingObject");
          if(courseListing == null) {
            context.fail("No course listing object found");
            return;
          }
          if(!courseListing.getString("id").equals(COURSE_LISTING_1_ID)) {
            context.fail("Bad id for course listing object, got " +
                courseListing.getString("id") + " expected " + COURSE_LISTING_1_ID);
          }
          if(course.getJsonObject("departmentObject") == null) {
            context.fail("No department object found");
            return;
          }
          if(!course.getJsonObject("departmentObject").getString("id").equals(DEPARTMENT_1_ID)) {
            context.fail("Bad id for department object, got " +
                course.getJsonObject("departmentObject").getString("id") +
                " expected " + DEPARTMENT_1_ID);
          }
          if(courseListing.getJsonObject("courseTypeObject") == null) {
            context.fail("No course type object found in json " + course.encode());
            return;
          }
          if(!courseListing.getJsonObject("courseTypeObject").getString("id").equals(COURSE_TYPE_1_ID)) {
            context.fail("Bad id for course type object, got " +
                courseListing.getJsonObject("courseTypeId").getString("id") +
                " expected " + COURSE_TYPE_1_ID);
          }
          JsonArray instructorObjects = courseListing.getJsonArray("instructorObjects");
          if(instructorObjects == null) {
            context.fail("No instructor objects found in courselisting");
            return;
          }
          if(instructorObjects.size() < 2) {
            context.fail("Expected at least two instructor objects in courselisting: " + courseListing.encode());
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
        async.complete();
      }
    });
  }

  @Test
  public void putCourseListingById(TestContext context) {
    Async async = context.async();
    MultiMap acceptText = MultiMap.caseInsensitiveMultiMap();
    acceptText.add("Accept", "text/plain");
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_1_ID)
        .put("termId", TERM_2_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString());
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID,
        PUT, acceptText, courseListingJson.encode(), 204, "Put CourseListing 1")
        .setHandler( res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
            , GET, standardHeaders, null, 200, "Get courselisting by id").setHandler(
                res2 -> {
          if(res2.failed()) {
            context.fail(res2.cause());
          } else {
            try {
              JsonObject courseListing = res2.result().getJson();
              if(!courseListing.getString("termId").equals(TERM_2_ID)) {
                context.fail("Bad term id for courselisting after put");
                return;
              }
            } catch(Exception e) {
              context.fail(e);
            }
            async.complete();
          }
        });
      }
    });
  }

  @Test
  public void putCourseListingByIdWithScrubbedField(TestContext context) {
    Async async = context.async();
    MultiMap acceptText = MultiMap.caseInsensitiveMultiMap();
    acceptText.add("Accept", "text/plain");
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_1_ID)
        .put("termId", TERM_2_ID)
        .put("termObject", new JsonObject().put("id", TERM_1_ID).put("name", "whatever")
          .put("startDate", "2020-01-01").put("endDate","2000-01-01"))
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("courseTypeObject", new JsonObject().put("id", COURSE_TYPE_2_ID).put("name","whatever"))
        .put("locationId", OkapiMock.location1Id)
        .put("locationObject", new JsonObject().put("id", OkapiMock.location2Id))
        .put("externalId", UUID.randomUUID().toString());
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID,
        PUT, acceptText, courseListingJson.encode(), 204, "Put CourseListing 1")
        .setHandler( res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
            , GET, standardHeaders, null, 200, "Get courselisting by id").setHandler(
                res2 -> {
          if(res2.failed()) {
            context.fail(res2.cause());
          } else {
            try {
              JsonObject courseListing = res2.result().getJson();
              context.assertEquals(courseListing.getString("termId"), TERM_2_ID);
              context.assertEquals(courseListing.getJsonObject("termObject")
                  .getString("id"), TERM_2_ID);
              context.assertEquals(courseListing.getString("locationId"),
                  OkapiMock.location1Id);
              context.assertEquals(courseListing.getJsonObject("locationObject")
                  .getString("id"), OkapiMock.location1Id);
              context.assertEquals(courseListing.getString("courseTypeId"),
                  COURSE_TYPE_1_ID);
              context.assertEquals(courseListing.getJsonObject("courseTypeObject")
                  .getString("id"), COURSE_TYPE_1_ID);
            } catch(Exception e) {
              context.fail(e);
            }
            async.complete();
          }
        });
      }
    });
  }

  @Test
  public void postInstructorToCourseListing(TestContext context) {
    Async async = context.async();
    JsonObject instructorJson = new JsonObject()
        .put("id", UUID.randomUUID().toString())
        .put("name", "Stainless Steel Rat")
        .put("userId", OkapiMock.user1Id)
        .put("courseListingId", COURSE_LISTING_1_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/instructors",
        POST, standardHeaders, instructorJson.encode(), 201, "Post Instructor to Course Listing")
        .setHandler( postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/"
          + COURSE_LISTING_1_ID + "/instructors/" + instructorJson.getString("id"),
          GET, standardHeaders, null, 200, "Get instructor from courselisting by id").setHandler(
            getCLInstructorsRes -> {
          if(getCLInstructorsRes.failed()) {
            context.fail(getCLInstructorsRes.cause());
          } else {
            try {
              JsonObject returnedInstructorJson = getCLInstructorsRes.result().getJson();
              if(!returnedInstructorJson.getString("id").equals(instructorJson.getString("id"))) {
                context.fail("Returned instructor does not match that which was POSTed");
                return;
              }
              if(!returnedInstructorJson.getJsonObject("patronGroupObject")
                  .getString("id").equals(OkapiMock.group1Id)) {
                context.fail("Expected id '" + OkapiMock.group1Id + "' for patron group id");
                return;
              }
              if(!returnedInstructorJson.getString("patronGroup").equals(OkapiMock.group1Id)) {
                context.fail("Expected id '" + OkapiMock.group1Id + "' for patronGroup field");
                return;
              }
              if(!returnedInstructorJson.getString("barcode").equals(OkapiMock.barcode1)) {
                context.fail("Expected barcode '" + OkapiMock.barcode1 + "' for barcode field, got '"
                  + returnedInstructorJson.getString("barcode") + "'");
                return;
              }
            } catch(Exception e) {
              context.fail(e);
            }
            TestUtil.doRequest(vertx, baseUrl + "/courses/" +
                COURSE_1_ID, GET, standardHeaders, null, 200,
                "Get course by courselisting id").setHandler(getCourseRes -> {
              if(getCourseRes.failed()) {
                context.fail(getCourseRes.cause());
              } else {
                try {
                  JsonArray instructorObjects = getCourseRes.result().getJson()
                      .getJsonObject("courseListingObject").getJsonArray("instructorObjects");
                  if(instructorObjects.isEmpty()) {
                    context.fail("No instructorObjects found in " + getCourseRes.result().getBody());
                    return;
                  }
                  boolean found = false;
                  for(int i = 0; i < instructorObjects.size(); i++) {
                    JsonObject instructorObjectJson = instructorObjects.getJsonObject(i);
                    JsonObject patronGroupObject = instructorObjectJson.getJsonObject("patronGroupObject");
                    if(patronGroupObject != null &&
                      patronGroupObject.getString("id").equals(OkapiMock.group1Id)) {
                      found = true;
                      break;
                    }
                  }
                  if(!found) {
                    context.fail("Could not find patronGroupObject in instructor with id matching "
                      + OkapiMock.group1Id);
                    return;
                  }
                } catch(Exception e) {
                  context.fail(e);
                }
                async.complete();
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void postReserveToCourseListing(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {
        JsonObject reserveJson = res.result().getJson();
        if(!reserveJson.containsKey("copiedItem") ||
            reserveJson.getJsonObject("copiedItem") == null) {
          context.fail("No copiedItem field found");
          return;
        }
        JsonObject itemJson = reserveJson.getJsonObject("copiedItem");
        if(! itemJson.getString("barcode").equals(OkapiMock.barcode1)) {
          context.fail("Expected bardcode " + OkapiMock.barcode1 + " got " +
              itemJson.getString("barcode"));
          return;
        }
        if(! itemJson.getString("title").equals(OkapiMock.title1)) {
          context.fail("Expected title" + OkapiMock.title1 + " got " +
              itemJson.getString("title"));
          return;
        }
        if(! itemJson.getString("temporaryLocationId").equals(OkapiMock.location2Id)) {
          context.fail("Expected temporaryLocationId" + OkapiMock.location2Id + " got " +
              itemJson.getString("temporaryLocationId"));
          return;
        }
        if(itemJson.getString("copy") == null || ! itemJson.getString("copy").equals(OkapiMock.copy1)) {
          context.fail("Expected copy " + OkapiMock.copy1 + " got " +
              itemJson.getString("copy"));
          return;
        }

        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" +
            COURSE_LISTING_1_ID + "/reserves/" + reserveJson.getString("id"),
            GET, standardHeaders, null, 200, "Get Posted Reserve").setHandler(getRes -> {
              if(getRes.failed()) {
                context.fail(getRes.cause());
              } else {
                JsonObject getReserveJson = getRes.result().getJson();
                JsonObject getCopiedItemJson = getReserveJson.getJsonObject("copiedItem");
                context.assertEquals(getCopiedItemJson.getString("instanceId"), OkapiMock.instance1Id);
                context.assertEquals(getCopiedItemJson.getString("instanceHrid"), OkapiMock.instance1Hrid);
                context.assertEquals(getCopiedItemJson.getString("holdingsId"), OkapiMock.holdings1Id);
                JsonObject permanentLocationJson = getCopiedItemJson.getJsonObject("permanentLocationObject");
                JsonObject temporaryLocationJson = getCopiedItemJson.getJsonObject("temporaryLocationObject");
                JsonObject temporaryLoanTypeJson = getReserveJson.getJsonObject("temporaryLoanTypeObject");
                JsonObject copyrightTrackingJson = getReserveJson.getJsonObject("copyrightTracking");
                JsonObject processingStatusJson = getReserveJson.getJsonObject("processingStatusObject");
                if(permanentLocationJson == null || temporaryLocationJson == null ) {
                  context.fail("Null result for permanent or temporary location object");
                  return;
                }
                if(!permanentLocationJson.getString("id").equals(OkapiMock.location1Id)) {
                  context.fail("Expected permanentLocationObject with id " + OkapiMock.location1Id);
                  return;
                }
                if(!temporaryLocationJson.getString("id").equals(OkapiMock.location2Id)) {
                  context.fail("Expected temporaryLocationObject with id " + OkapiMock.location2Id);
                  return;
                }
                if(temporaryLoanTypeJson == null) {
                  context.fail("No temporaryLoanTypeObject found in result");
                  return;
                }
                if(!temporaryLoanTypeJson.getString("id").equals(OkapiMock.loanType1Id)) {
                  context.fail("Retrieved loan type id does not match existing");
                  return;
                }
                if(copyrightTrackingJson == null) {
                  context.fail("No copyrightTracking object found in result");
                  return;
                }
                JsonObject copyrightStatusJson = copyrightTrackingJson.getJsonObject("copyrightStatusObject");
                if(copyrightStatusJson == null) {
                  context.fail("No copyrightStatus object found in result");
                  return;
                }
                if(copyrightStatusJson.getString("id") == null || !copyrightStatusJson.getString("id").equals(COPYRIGHT_STATUS_1_ID)) {
                  context.fail("Retrieved copyright status id does not match existing");
                  return;
                }
                if(processingStatusJson == null) {
                  context.fail("No copyrightStatus object found in result");
                  return;
                }
                if(processingStatusJson.getString("id") == null || !processingStatusJson.getString("id").equals(PROCESSING_STATUS_1_ID)) {
                  context.fail("Retrieved processing status id does not match existing");
                  return;
                }
              }
              async.complete();
            });
       }
    });
  }

  @Test
  public void getReservesFromCourseListingsWithBadQuery(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves?query=NOT+blooh", GET, standardHeaders, null, 500,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void postReserveToCourseListingWithBarcode(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
        )
        .put("copiedItem", new JsonObject()
          .put("barcode", OkapiMock.barcode1)
        );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {
        JsonObject reserveJson = null;
        try {
          logger.info("Post successful, checking results");
          reserveJson = res.result().getJson();
          if(!reserveJson.containsKey("copiedItem") ||
              reserveJson.getJsonObject("copiedItem") == null) {
            context.fail("No copiedItem field found");
            return;
          }
          if(reserveJson.getString("itemId") == null
              || !reserveJson.getString("itemId").equals(OkapiMock.item1Id)) {
            context.fail("Expected item id " + OkapiMock.item1Id + " got " +
                reserveJson.getString("itemId"));
          }
          JsonObject copiedItemJson = reserveJson.getJsonObject("copiedItem");

          if(! copiedItemJson.getString("barcode").equals(OkapiMock.barcode1)) {
            context.fail("Expected barcode " + OkapiMock.barcode1 + " got " +
                copiedItemJson.getString("barcode"));
            return;
          }
          if(copiedItemJson.getString("title") == null ||
              !copiedItemJson.getString("title").equals(OkapiMock.title1)) {
            context.fail("Expected title" + OkapiMock.title1 + " got " +
                copiedItemJson.getString("title"));
            return;
          }
          if(copiedItemJson.getString("temporaryLocationId") == null
             || !copiedItemJson.getString("temporaryLocationId").equals(OkapiMock.location2Id)) {
            context.fail("Expected temporaryLocationId" + OkapiMock.location2Id + " got " +
                copiedItemJson.getString("temporaryLocationId"));
            return;
          }
          if(copiedItemJson.getString("copy") == null || ! copiedItemJson.getString("copy").equals(OkapiMock.copy1)) {
            context.fail("Expected copy " + OkapiMock.copy1 + " got " +
                copiedItemJson.getString("copy"));
            return;
          }
        } catch(Exception e) {
          context.fail(e);
          return;
        }

        logger.info("Requesting new reserve to test populated values");

        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" +
            COURSE_LISTING_1_ID + "/reserves/" + reserveJson.getString("id"),
            GET, standardHeaders, null, 200, "Get Posted Reserve").setHandler(getRes -> {
              if(getRes.failed()) {
                context.fail(getRes.cause());
              } else {
                JsonObject getReserveJson = getRes.result().getJson();
                JsonObject getItemJson = getReserveJson.getJsonObject("copiedItem");
                JsonObject permanentLocationJson = getItemJson.getJsonObject("permanentLocationObject");
                JsonObject temporaryLocationJson = getItemJson.getJsonObject("temporaryLocationObject");
                JsonObject temporaryLoanTypeJson = getReserveJson.getJsonObject("temporaryLoanTypeObject");
                JsonObject copyrightTrackingJson = getReserveJson.getJsonObject("copyrightTracking");
                JsonObject processingStatusJson = getReserveJson.getJsonObject("processingStatusObject");
                if(permanentLocationJson == null || temporaryLocationJson == null ) {
                  context.fail("Null result for permanent or temporary location object");
                  return;
                }
                if(!permanentLocationJson.getString("id").equals(OkapiMock.location1Id)) {
                  context.fail("Expected permanentLocationObject with id " + OkapiMock.location1Id);
                  return;
                }
                if(!temporaryLocationJson.getString("id").equals(OkapiMock.location2Id)) {
                  context.fail("Expected temporaryLocationObject with id " + OkapiMock.location2Id);
                  return;
                }
                if(temporaryLoanTypeJson == null) {
                  context.fail("No temporaryLoanTypeObject found in result");
                  return;
                }
                if(!temporaryLoanTypeJson.getString("id").equals(OkapiMock.loanType1Id)) {
                  context.fail("Retrieved loan type id does not match existing");
                  return;
                }
                if(copyrightTrackingJson == null) {
                  context.fail("No copyrightTracking object found in result");
                  return;
                }
                JsonObject copyrightStatusJson = copyrightTrackingJson.getJsonObject("copyrightStatusObject");
                if(copyrightStatusJson == null) {
                  context.fail("No copyrightStatus object found in result");
                  return;
                }
                if(copyrightStatusJson.getString("id") == null || !copyrightStatusJson.getString("id").equals(COPYRIGHT_STATUS_1_ID)) {
                  context.fail("Retrieved copyright status id does not match existing");
                  return;
                }
                if(processingStatusJson == null) {
                  context.fail("No copyrightStatus object found in result");
                  return;
                }
                if(processingStatusJson.getString("id") == null || !processingStatusJson.getString("id").equals(PROCESSING_STATUS_1_ID)) {
                  context.fail("Retrieved processing status id does not match existing");
                  return;
                }
              }
              async.complete();
            });
      }
    });
  }

  @Test
  public void postReserveToCourseListingWithBadBarcode(TestContext context) {
    Async async = context.async();
    String badBarcode = "123456";
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
        )
        .put("copiedItem", new JsonObject()
          .put("barcode", badBarcode)
        );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 400,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {

          async.complete();
      }
    });
  }

  @Test
  public void postReserveToCourseListingWithGoodItemAndBadBarcode(TestContext context) {
    Async async = context.async();
    String badBarcode = "123456";
    JsonObject reservePostJson = new JsonObject()
        .put("itemId", OkapiMock.item1Id )
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
        )
        .put("copiedItem", new JsonObject()
          .put("barcode", badBarcode)
        );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 400,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {
        try {
          async.complete();
        } catch(Exception e) {
          context.fail(e);
          return;
        }
      }
    });
  }

  @Test
  public void postReserveToCourseListingWithBadItem(TestContext context) {
    Async async = context.async();
    String badItemId = UUID.randomUUID().toString();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", badItemId)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
        );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 400,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {
          async.complete();
      }
    });
  }

  @Test
  public void postReserveToCourseListingWithBogusItem(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", UUID.randomUUID().toString());
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 400,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {

        async.complete();
      }
    });
  }


  //This has to return 422 because of foreign key relationship
  @Test
  public void postReserveToCourseListingWithBogusCopyrightAndProcessingStatuses(
      TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", UUID.randomUUID().toString())
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", UUID.randomUUID().toString()));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 422,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {
        /*
        JsonObject reserveJson = res.result().getJson();
        if(!reserveJson.containsKey("copiedItem") ||
            reserveJson.getJsonObject("copiedItem") == null) {
          context.fail("No copiedItem field found");
          return;
        }
        */
        async.complete();
      }
    });
  }

  @Test
  public void postReserveToCourseListingWithTemporaryLocationId(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("copiedItem", new JsonObject()
          .put("temporaryLocationId", OkapiMock.location1Id));

    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        JsonObject postResponseJson = res.result().getJson();
        JsonObject copiedItemJson = postResponseJson.getJsonObject("copiedItem");
        context.assertNotNull(copiedItemJson);
        context.assertEquals(OkapiMock.location1Id, copiedItemJson.getString("temporaryLocationId"));
        CRUtil.lookupItemHoldingsInstanceByItemId(OkapiMock.item1Id,
            okapiHeaders, vertx.getOrCreateContext()).setHandler(lookupRes -> {
          if(lookupRes.failed()) {
            context.fail(lookupRes.cause());
          } else {
            JsonObject itemJson = lookupRes.result().getJsonObject("item");
            context.assertEquals(OkapiMock.location1Id, itemJson.getString("temporaryLocationId"));
            async.complete();
          }
        });
      }
    });
  }

  @Test
  public void deleteReserveFromCourseListing(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("copiedItem", new JsonObject()
          .put("temporaryLocationId", OkapiMock.location1Id));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
        "Post Course Reserve").compose(f -> {
      return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, DELETE, acceptTextHeaders, reservePostJson.encode(), 204,
        "Delete Course Reserve");
    }).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        CRUtil.lookupItemHoldingsInstanceByItemId(OkapiMock.item1Id,
            okapiHeaders, vertx.getOrCreateContext()).setHandler(lookupRes -> {
          if(lookupRes.failed()) {
            context.fail(lookupRes.cause());
          } else {
            try {
              JsonObject itemJson = lookupRes.result().getJsonObject("item");
              context.assertNull(itemJson.getString("temporaryLocationId"));
              async.complete();
            } catch(Exception e) {
              context.fail(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void postReservesToCourseListingTestRetrieval(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson1 = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    JsonObject reservePostJson2 = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item2Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    Future<WrappedResponse> postFuture = TestUtil.doRequest(vertx, baseUrl
        + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").compose(w -> {
          return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
              "/reserves", POST, standardHeaders, reservePostJson2.encode(), 201,
              "Post Course Reserve");
        });
    postFuture.setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
        PostgresClient pgClient = PostgresClient.getInstance(vertx, "diku");
        String courseListingQueryClause = String.format("courseListingId = %s", COURSE_LISTING_1_ID);
        CQLWrapper filter = getCQL(courseListingQueryClause, 10, 0, RESERVES_TABLE);
        pgClient.get(RESERVES_TABLE, Reserve.class, filter, true, getReply -> {
          if(getReply.failed()) {
            context.fail(getReply.cause());
          } else {
            List<Reserve> reserveList = getReply.result().getResults();
            if(reserveList.size() != 2) {
              context.fail("Expected 2 results");
              return;
            }
            Context vertxContext = vertx.getOrCreateContext();
            CRUtil.expandListOfReserves(reserveList, okapiHeaders, vertxContext)
                .setHandler(expandRes -> {
              if(expandRes.failed()) {
                context.fail(expandRes.cause());
              } else {
                for(Reserve reserve : expandRes.result()) {
                  if(reserve.getProcessingStatusObject() == null) {
                    context.fail("Expected processing status object to be populated");
                    return;
                  }
                  if(reserve.getTemporaryLoanTypeObject() == null) {
                    context.fail("Expected temporary loan type object to be populated");
                    return;
                  }
                }
                async.complete();
              }
            });
          }
        });
        } catch(Exception e) {
          context.fail(e);
          return;
        }
      }
    });
  }


  @Test
  public void postReservesToCourseListingTestRetrievalAPI(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson1 = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    JsonObject reservePostJson2 = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item2Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    Future<WrappedResponse> postFuture = TestUtil.doRequest(vertx, baseUrl
        + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").compose(w -> {
          return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
              "/reserves", POST, standardHeaders, reservePostJson2.encode(), 201,
              "Post Course Reserve");
        });
    postFuture.setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
              "/reserves?expand=*", GET, standardHeaders, null, 200, "Get Course Reserves")
              .setHandler(getRes -> {
            if(getRes.failed()) {
              context.fail(getRes.cause());
            } else {
              try {
                JsonObject reserveResult = getRes.result().getJson();
                if(reserveResult.getInteger("totalRecords") != 2) {
                  context.fail("Expected two records in result");
                  return;
                }
                JsonArray reserves = reserveResult.getJsonArray("reserves");
                for(Object ob : reserves) {
                  if( ((JsonObject)ob).getJsonObject("copiedItem") == null ) {
                    context.fail("No copied item found in result " + ((JsonObject)ob).encode());
                    return;
                  }
                  if( ((JsonObject)ob).getJsonObject("copiedItem").getString("barcode") == null) {
                    context.fail("No barcode found in result " + ((JsonObject)ob).encode());
                    return;
                  }
                  if( ((JsonObject)ob).getJsonObject("temporaryLoanTypeObject") == null ) {
                    context.fail("No temporary loan type object found in result " + ((JsonObject)ob).encode());
                    return;
                  }
                  context.assertEquals(((JsonObject)ob).getJsonObject("temporaryLoanTypeObject").getString("id"), OkapiMock.loanType1Id);
                  if( ((JsonObject)ob).getJsonObject("processingStatusObject") == null ) {
                    context.fail("No processing status object found in result " + ((JsonObject)ob).encode());
                    return;
                  }
                  context.assertEquals(((JsonObject)ob).getJsonObject("processingStatusObject").getString("id"), PROCESSING_STATUS_1_ID);
                }
              } catch(Exception e) {
                context.fail(e);
                return;
              }
              async.complete();
            }
          });
        } catch(Exception e) {
          context.fail(e);
          return;
        }
      }
    });
  }

  @Test
  public void postReservesToCourseListingTestRetrievalAPINoExpand(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson1 = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    JsonObject reservePostJson2 = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item2Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    Future<WrappedResponse> postFuture = TestUtil.doRequest(vertx, baseUrl
        + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").compose(w -> {
          return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
              "/reserves", POST, standardHeaders, reservePostJson2.encode(), 201,
              "Post Course Reserve");
        });
    postFuture.setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
              "/reserves", GET, standardHeaders, null, 200, "Get Course Reserves")
              .setHandler(getRes -> {
            if(getRes.failed()) {
              context.fail(getRes.cause());
            } else {
              try {
                JsonObject reserveResult = getRes.result().getJson();
                if(reserveResult.getInteger("totalRecords") != 2) {
                  context.fail("Expected two records in result");
                  return;
                }
                JsonArray reserves = reserveResult.getJsonArray("reserves");
                for(Object ob : reserves) {
                  if( ((JsonObject)ob).getJsonObject("temporaryLoanTypeObject") != null ) {
                    context.fail("Did not expect temporary loan type object in result " + ((JsonObject)ob).encode());
                    return;
                  }
                  if( ((JsonObject)ob).getJsonObject("processingStatusObject") != null ) {
                    context.fail("Did not expect processing status object in result " + ((JsonObject)ob).encode());
                    return;
                  }

                }
              } catch(Exception e) {
                context.fail(e);
                return;
              }
              async.complete();
            }
          });
        } catch(Exception e) {
          context.fail(e);
          return;
        }
      }
    });
  }

  @Test
  public void getUser(TestContext context) {
    Async async = context.async();
    String userId = OkapiMock.user1Id;
    TestUtil.doRequest(vertx, okapiUrl + "/users/" + userId, GET, null, null,
        200, "Get user from mock okapi").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject userJson = res.result().getJson();
          if(!userJson.getString("id").equals(userId)) {
            context.fail("Returned user id does not match " + userId);
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getGroup(TestContext context) {
    Async async = context.async();
    String groupId = OkapiMock.group1Id;
    TestUtil.doRequest(vertx, okapiUrl + "/groups/" + groupId, GET, null, null,
        200, "Get group from mock okapi").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject groupJson = res.result().getJson();
          if(!groupJson.getString("id").equals(groupId)) {
            context.fail("Returned user id does not match " + groupId);
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getUserAndPatronGroupFromUserId(TestContext context) {
    Async async = context.async();
    CRUtil.lookupUserAndGroupByUserId(OkapiMock.user1Id, okapiHeaders,
        vertx.getOrCreateContext()).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject json = res.result();
          if(!json.getJsonObject("group").getString("id").equals(OkapiMock.group1Id)) {
            context.fail("Retrieved Group ID does not match");
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getItemHoldingsInstanceFromItemId(TestContext context) {
    Async async = context.async();
    CRUtil.lookupItemHoldingsInstanceByItemId(OkapiMock.item1Id, okapiHeaders,
        vertx.getOrCreateContext()).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject result = res.result();
          JsonObject itemJson = result.getJsonObject("item");
          JsonObject holdingsJson = result.getJsonObject("holdings");
          JsonObject instanceJson = result.getJsonObject("instance");
          if(!itemJson.getString("id").equals(OkapiMock.item1Id)) {
            context.fail("Retrieved item id does not match");
            return;
          }
          if(!instanceJson.getString("id").equals(OkapiMock.instance1Id)) {
            context.fail("Retrieved instance id does not match");
            return;
          }
          if(!holdingsJson.getString("id").equals(OkapiMock.holdings1Id)) {
            context.fail("Retrieved holdings id does not match");
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getItemByBarcode(TestContext context) {
    Async async = context.async();
    CRUtil.lookupItemByBarcode(OkapiMock.barcode1, okapiHeaders,
        vertx.getOrCreateContext()).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject result = res.result();
          if(!result.getString("id").equals(OkapiMock.item1Id)) {
            context.fail("Retrieved item does not match expect " + OkapiMock.item1Id);
            return;
          }
        async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void loadAndRetrieveCourseListingWithLocation(TestContext context) {
    Async async = context.async();
    String courseListingId = UUID.randomUUID().toString();
    JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString())
        .put("locationId", OkapiMock.location1Id);
    Future<WrappedResponse> clFuture = TestUtil.doRequest(vertx, baseUrl + "/courselistings",
        POST, standardHeaders, courseListingJson.encode(), 201, "Post CourseListing With Location")
          .compose(res -> {
              JsonObject courseJson = new JsonObject()
                  .put("id", UUID.randomUUID().toString())
                  .put("departmentId", DEPARTMENT_1_ID)
                  .put("courseListingId", courseListingId)
                  .put("name", "Bogus Test Course");
              return TestUtil.doRequest(vertx, baseUrl + "/courses", POST, standardHeaders,
                  courseJson.encode(), 201, "Post Course with new Course Listing");
        }).compose(res -> {
          String courseId = res.getJson().getString("id");
          return TestUtil.doRequest(vertx, baseUrl + "/courses/" + courseId,
              GET, standardHeaders, null, 200, "Get newly created Course");
        }).setHandler(res -> {
          if(res.failed()) {
          context.fail(res.cause());
          } else {
            JsonObject resultJson = res.result().getJson();
            JsonObject clJson = resultJson.getJsonObject("courseListingObject");
            if(clJson == null) {
              context.fail("No courseListingObject found in result");
            } else if(!clJson.containsKey("locationObject")) {
              context.fail("No location object in result: " + resultJson.encode());
            } else if(clJson.getJsonObject("locationObject") == null) {
              context.fail("Null location object result");
            } else if(!clJson.getJsonObject("locationObject")
                .getString("id").equals(OkapiMock.location1Id)) {
              context.fail("Returned id for locationObject does not match");
            } else {
              async.complete();
            }
          }
        });
  }

  @Test
  public void loadAndRetrieveCourseListingWithNonExistantLocation(TestContext context) {
    Async async = context.async();
    String courseListingId = UUID.randomUUID().toString();
    String fakeLocationId = UUID.randomUUID().toString();
    JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString())
        .put("locationId", fakeLocationId);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, standardHeaders,
        courseListingJson.encode(), 201, "Post CourseListing with fake location id")
        .compose(w -> {
          JsonObject courseJson = new JsonObject()
            .put("id", UUID.randomUUID().toString())
            .put("departmentId", DEPARTMENT_1_ID)
            .put("courseListingId", courseListingId)
            .put("name", "Bogus Test Course");
          return TestUtil.doRequest(vertx, baseUrl + "/courses", POST, standardHeaders,
              courseJson.encode(), 201, "Post Course with new Course Listing");
        })
        .compose(w -> {
          String courseId = w.getJson().getString("id");
          return TestUtil.doRequest(vertx, baseUrl + "/courses/" + courseId, GET,
              standardHeaders, null, 200, "Get newly created course record");
        }).setHandler(w -> {
          if(w.failed()) {
            context.fail(w.cause());
          } else {
            JsonObject resultJson = w.result().getJson();
            JsonObject clJson = resultJson.getJsonObject("courseListingObject");
            if(clJson == null) {
              context.fail("No courseListingObject found in result json");
            } else if(clJson.containsKey("locationObject")) {
              context.fail("Result should not contain a location object");
            } else {
              async.complete();
            }
          }
        });
  }

  @Test
  public void loadAndRetrieveCourseListingWithServicePoint(TestContext context) {
    Async async = context.async();
    String courseListingId = UUID.randomUUID().toString();
    JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString())
        .put("servicepointId", OkapiMock.servicePoint1Id);
    Future<WrappedResponse> clFuture = TestUtil.doRequest(vertx, baseUrl + "/courselistings",
        POST, standardHeaders, courseListingJson.encode(), 201,
        "Post CourseListing With Service Point")
          .compose(res -> {
              JsonObject courseJson = new JsonObject()
                  .put("id", UUID.randomUUID().toString())
                  .put("departmentId", DEPARTMENT_1_ID)
                  .put("courseListingId", courseListingId)
                  .put("name", "Bogus Test Course");
              return TestUtil.doRequest(vertx, baseUrl + "/courses", POST, standardHeaders,
                  courseJson.encode(), 201, "Post Course with new Course Listing");
        }).compose(res -> {
          String courseId = res.getJson().getString("id");
          return TestUtil.doRequest(vertx, baseUrl + "/courses/" + courseId,
              GET, standardHeaders, null, 200, "Get newly created Course");
        }).setHandler(res -> {
          if(res.failed()) {
          context.fail(res.cause());
          } else {
            JsonObject resultJson = res.result().getJson();
            JsonObject clJson = resultJson.getJsonObject("courseListingObject");
            if(clJson == null) {
              context.fail("No courseListingObject found in result");
            } else if(!clJson.containsKey("servicepointObject")) {
              context.fail("No service point object in result: " + resultJson.encode());
            } else if(clJson.getJsonObject("servicepointObject") == null) {
              context.fail("Null service point object result");
            } else if(!clJson.getJsonObject("servicepointObject")
                .getString("id").equals(OkapiMock.servicePoint1Id)) {
              context.fail("Returned id for service point object does not match");
            } else if(!clJson.getJsonObject("servicepointObject")
                .getJsonArray("staffSlips").getJsonObject(0).getString("id")
                .equals(OkapiMock.staffSlip1Id)) {
              context.fail("Expected Staff Slip ID does not match");
            } else {
              async.complete();
            }
          }
        });

  }

  @Test
  public void loadAndRetrieveCourseListingWithNonExistantServicepoint(TestContext context) {
    Async async = context.async();
    String courseListingId = UUID.randomUUID().toString();
    String fakeServicepointId = UUID.randomUUID().toString();
    JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString())
        .put("servicepointId", fakeServicepointId);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, standardHeaders,
        courseListingJson.encode(), 201, "Post CourseListing with fake location id")
        .compose(w -> {
          JsonObject courseJson = new JsonObject()
            .put("id", UUID.randomUUID().toString())
            .put("departmentId", DEPARTMENT_1_ID)
            .put("courseListingId", courseListingId)
            .put("name", "Bogus Test Course");
          return TestUtil.doRequest(vertx, baseUrl + "/courses", POST, standardHeaders,
              courseJson.encode(), 201, "Post Course with new Course Listing");
        })
        .compose(w -> {
          String courseId = w.getJson().getString("id");
          return TestUtil.doRequest(vertx, baseUrl + "/courses/" + courseId, GET,
              standardHeaders, null, 200, "Get newly created course record");
        }).setHandler(w -> {
          if(w.failed()) {
            context.fail(w.cause());
          } else {
            JsonObject resultJson = w.result().getJson();
            JsonObject clJson = resultJson.getJsonObject("courseListingObject");
            if(clJson == null) {
              context.fail("No courseListingObject found in result json");
            } else if(clJson.containsKey("servicepointObject")) {
              context.fail("Result should not contain a service point object");
            } else {
              async.complete();
            }
          }
        });
  }

  @Test
  public void postReserveToCourseListingWithOldStyleCopyItemField(TestContext context) {
    Async async = context.async();
    JsonObject reservePostJson = new JsonObject()
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item2Id);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
        "Post Course Reserve").setHandler(res -> {
      if(res.failed()) {
         context.fail(res.cause());
       } else {
        JsonObject reserveJson = res.result().getJson();
        if(!reserveJson.containsKey("copiedItem") ||
            reserveJson.getJsonObject("copiedItem") == null) {
          context.fail("No copiedItem field found");
          return;
        }
        JsonObject itemJson = reserveJson.getJsonObject("copiedItem");
        if(! itemJson.getString("barcode").equals(OkapiMock.barcode2)) {
          context.fail("Expected bardcode " + OkapiMock.barcode2 + " got " +
              itemJson.getString("barcode"));
          return;
        }
        if(! itemJson.getString("title").equals(OkapiMock.title1)) {
          context.fail("Expected title" + OkapiMock.title1 + " got " +
              itemJson.getString("title"));
          return;
        }
        if(! itemJson.getString("temporaryLocationId").equals(OkapiMock.location1Id)) {
          context.fail("Expected temporaryLocationId " + OkapiMock.location1Id + " got " +
              itemJson.getString("temporaryLocationId"));
          return;
        }
        if(itemJson.getString("copy") == null || ! itemJson.getString("copy").equals(OkapiMock.copy1)) {
          context.fail("Expected copy " + OkapiMock.copy1 + " got " +
              itemJson.getString("copy"));
          return;
        }
        async.complete();
       }
    });
  }


  @Test
  public void deleteAllReservesForCourseListing(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item2Id);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
         context.fail(postRes.cause());
       } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, GET, standardHeaders, null, 200, "Get reserve")
            .setHandler(getRes -> {
          if(getRes.failed()) {
            context.fail(getRes.cause());
          } else {
            TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
                "/reserves", DELETE, standardHeaders, null, 204,
                "Delete reserves with courselisting " + COURSE_LISTING_1_ID)
                .setHandler(deleteRes -> {
              if(deleteRes.failed()) {
                context.fail(deleteRes.cause());
              } else {
                TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
                 "/reserves/" + reserveId, GET, standardHeaders, null, 404, "Get reserve again")
                   .setHandler(getAgainRes-> {
                   if(getAgainRes.failed()) {
                     context.fail(getAgainRes.cause());
                   } else {
                     logger.info("getAgainRes succeeded");
                     async.complete();
                   }
                 });
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void testRoles(TestContext context) {
    Async async = context.async();
    String roleId = UUID.randomUUID().toString();
    JsonObject roleJson = new JsonObject()
        .put("id", roleId)
        .put("name", "newrole");
    JsonObject roleModJson = new JsonObject()
        .put("id", roleId)
        .put("name", "oldrole");
    String postUrl = baseUrl + "/roles";
    String getUrl = baseUrl + "/roles/" + roleId;
    String putUrl = getUrl;
    String deleteUrl = getUrl;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(roleJson, roleModJson, postUrl, getUrl, putUrl, deleteUrl,
        deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testCoursesForCourseListing(TestContext context) {
    Async async = context.async();
    String courseId = UUID.randomUUID().toString();
    JsonObject courseJson = new JsonObject()
        .put("id", courseId)
        .put("name", "Basket Weaving")
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("departmentId", DEPARTMENT_1_ID);
    JsonObject courseModJson = new JsonObject()
        .put("id", courseId)
        .put("name", "Underwater Basket Weaving")
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("departmentId", DEPARTMENT_1_ID);
    String postUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/courses";
    String getUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/courses/" + courseId;
    String putUrl = getUrl;
    String deleteUrl = getUrl;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(courseJson, courseModJson, postUrl, getUrl, putUrl, deleteUrl,
        deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testInstructorsForCourseListing(TestContext context) {
    Async async = context.async();
    String instructorId = UUID.randomUUID().toString();
    JsonObject instructorJson = new JsonObject()
        .put("id", instructorId)
        .put("name", "John Brown")
        .put("courseListingId", COURSE_LISTING_1_ID);
    JsonObject instructorModJson = new JsonObject()
        .put("id", instructorId)
        .put("name", "Johann Brown")
        .put("courseListingId", COURSE_LISTING_1_ID);
    String postUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        + "/instructors";
    String getUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        + "/instructors/" + instructorId;
    String putUrl = getUrl;
    String deleteUrl = getUrl;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(instructorJson, instructorModJson, postUrl, getUrl,
        putUrl, deleteUrl, deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testProcessingStatuses(TestContext context) {
    Async async = context.async();
    String statusId = UUID.randomUUID().toString();
    JsonObject statusJson = new JsonObject()
        .put("id", statusId)
        .put("name", "status1");
    JsonObject statusModJson = new JsonObject()
        .put("id", statusId)
        .put("name", "status2");
    String postUrl = baseUrl + "/processingstatuses";
    String getUrl = baseUrl + "/processingstatuses/" + statusId;
    String putUrl = getUrl;
    String deleteUrl = getUrl;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(statusJson, statusModJson, postUrl, getUrl, putUrl, deleteUrl,
        deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testCopyrightStatuses(TestContext context) {
    Async async = context.async();
    String statusId = UUID.randomUUID().toString();
    JsonObject statusJson = new JsonObject()
        .put("id", statusId)
        .put("name", "status1");
    JsonObject statusModJson = new JsonObject()
        .put("id", statusId)
        .put("name", "status2");
    String postUrl = baseUrl + "/processingstatuses";
    String getUrl = baseUrl + "/processingstatuses/" + statusId;
    String putUrl = getUrl;
    String deleteUrl = getUrl;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(statusJson, statusModJson, postUrl, getUrl, putUrl, deleteUrl,
        deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testReservesForCourseListing(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reserveJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    JsonObject reserveModJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-31T00:00:00Z");
    String postUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        + "/reserves";
    String getUrl = postUrl + "/" + reserveId;
    String putUrl = getUrl;
    //String deleteUrl = getUrl;
    String deleteUrl = baseUrl + "/reserves/" + reserveId;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(reserveJson, reserveModJson, postUrl, getUrl, putUrl, deleteUrl,
        deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

   @Test
   public void testReservesForCourseListingWithBogusIds(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reserveJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", UUID.randomUUID().toString())
        .put("processingStatusId", UUID.randomUUID().toString())
        .put("temporaryLoanTypeId", UUID.randomUUID().toString())
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", UUID.randomUUID().toString()))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    JsonObject reserveModJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", UUID.randomUUID().toString())
        .put("processingStatusId", UUID.randomUUID().toString())
        .put("temporaryLoanTypeId", UUID.randomUUID().toString())
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", UUID.randomUUID().toString()))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2019-01-01T00:00:00Z");
    String postUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        + "/reserves";
    String getUrl = postUrl + "/" + reserveId;
    String putUrl = getUrl;
    String deleteUrl = getUrl;
    String deleteAllUrl = postUrl;
    testPostGetPutDelete(reserveJson, reserveModJson, postUrl, getUrl, putUrl, deleteUrl,
        deleteAllUrl).setHandler(res -> {
      if(res.failed()) {
        async.complete();
      } else {
        context.fail(res.cause());
      }
    });
   }

   @Test
   public void testGetReservesByCourseListingFail(TestContext context) {
     Async async = context.async();
     new CourseAPIFail()
         .getCoursereservesCourselistingsReservesByListingId(COURSE_LISTING_1_ID,
         "*", "flarglehonker = booom", 0, 10, null, okapiHeaders, res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         if(res.result().getStatus() != 500) {
           context.fail("Expected 500, got status " + res.result().getStatus());
           return;
         }
         async.complete();
       }
     }, vertx.getOrCreateContext());
   }

    @Test
   public void testGetReservesByCourseListingWTF(TestContext context) {
     Async async = context.async();
     new CourseAPIWTF()
         .getCoursereservesCourselistingsReservesByListingId(COURSE_LISTING_1_ID,
         "*", "flarglehonker = booom", 0, 10, null, okapiHeaders, res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         if(res.result().getStatus() != 500) {
           context.fail("Expected 500, got status " + res.result().getStatus());
           return;
         }
         async.complete();
       }
     }, vertx.getOrCreateContext());
   }

   @Test
   public void testDeleteInstructorsByListingIdFail(TestContext context) {
     Async async = context.async();
     new CourseAPIFail().deleteCoursereservesCourselistingsInstructorsByListingId(
         COURSE_LISTING_1_ID, okapiHeaders,
         res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         if(res.result().getStatus() != 500) {
           context.fail("Expected 500, got status " + res.result().getStatus());
           return;
         }
         async.complete();
       }
     }, vertx.getOrCreateContext());
   }

   @Test
   public void testDeleteCourselistings(TestContext context) {
     Async async = context.async();
     new CourseAPIFail().deleteCoursereservesCourselistings(okapiHeaders,
         res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         if(res.result().getStatus() != 500) {
           context.fail("Expected 500, got status " + res.result().getStatus());
           return;
         }
         async.complete();
       }
     }, vertx.getOrCreateContext());
   }

   @Test
   public void testDeleteReserveByIdFail(TestContext context) {
     Async async = context.async();
     new CourseAPIFail().deleteCoursereservesReservesByReserveId("blargh",
         null, okapiHeaders, res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         if(res.result().getStatus() != 500) {
           context.fail("Expected 500, got status " + res.result().getStatus());
           return;
         }
         async.complete();
       }
     }, vertx.getOrCreateContext());
   }

   @Test
   public void testDeleteBadReserveFail(TestContext context) {
     Async async = context.async();
     new CourseAPIFail().deleteReserve(UUID.randomUUID().toString(), okapiHeaders,
         vertx.getOrCreateContext()).setHandler(res -> {
       if(res.failed()) {
         async.complete();
       } else {
         context.fail("Expected delete reserve to fail");
       }
     });

   }

   @Test
   public void testDeleteReserveFail(TestContext context) {
     Async async = context.async();
     String reserveId = UUID.randomUUID().toString();
    JsonObject reserveJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
     String postUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        + "/reserves";
     TestUtil.doRequest(vertx, postUrl, POST, standardHeaders, reserveJson.encode(),
         201, "Post Reserve").setHandler(postRes -> {
      new CourseAPIFail().deleteReserve(UUID.randomUUID().toString(), okapiHeaders,
          vertx.getOrCreateContext()).setHandler(res -> {
        if(res.failed()) {
          async.complete();
        } else {
          context.fail("Expected delete reserve to fail");
        }
      });
     });
   }

   @Test
   public void testGetReservesFail(TestContext context) {
     Async async = context.async();
     String reserveId = UUID.randomUUID().toString();
    JsonObject reserveJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
     String postUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        + "/reserves";
     TestUtil.doRequest(vertx, postUrl, GET, standardHeaders, reserveJson.encode(),
         201, "Post Reserve").setHandler(postRes -> {
      new CourseAPIFail().handleGetReserves("*", null, 0, 1, okapiHeaders,
          reply -> {
            if(reply.result().getStatus() == 500) {
              async.complete();
            } else {
              context.fail("Expected get reserves to fail w/ 500");
            }
          },
          vertx.getOrCreateContext());
     });
   }

   @Test
   public void testGetExpandedCourseBadValues(TestContext context) {
     Async async = context.async();
     Course course = new Course();
     course.setId(UUID.randomUUID().toString());
     CRUtil.getExpandedCourse(course, okapiHeaders, vertx.getOrCreateContext())
         .setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
     });
   }

   /*
   @Test
   public void TestGetReservesByCourseListingBadQuery(TestContext context) {
     Async async = context.async();
     new CourseAPI()
         .getCoursereservesCourselistingsReservesByListingId(COURSE_LISTING_1_ID,
         "*", null, 0, 10, null, okapiHeaders, res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         if(res.result().getStatus() != 500) {
           context.fail("Expected 500, got status " + res.result().getStatus());
           return;
         }
         async.complete();
       }
     }, vertx.getOrCreateContext());
   }
   */



   @Test
   public void testGetPGClient(TestContext context) {
     Async async = context.async();
     PostgresClient pgClient = CRUtil.getPgClient(okapiHeaders,
         vertx.getOrCreateContext());
     context.assertTrue(pgClient != null);
     async.complete();
   }

   @Test
   public void testGetPGClientFromHeaders(TestContext context) {
     Async async = context.async();
     PostgresClient pgClient = new CourseAPI().getPGClientFromHeaders(
         vertx.getOrCreateContext(), okapiHeaders);
     context.assertTrue(pgClient != null);
     async.complete();
   }

   @Test
   public void testPutToItem(TestContext context) {
     Async async = context.async();
     String itemId = OkapiMock.item1Id;
     String newBarcode = "1112223334";
     JsonObject newItem = new JsonObject()
         .put("id", itemId)
         .put("status", new JsonObject().put("name", "Available"))
        .put("holdingsRecordId", OkapiMock.holdings1Id)
        .put("barcode",newBarcode)
        .put("volume", OkapiMock.volume1)
        .put("enumeration", OkapiMock.enumeration1)
        .put("copyNumber", OkapiMock.copy1)
        .put("electronicAccess", new JsonArray()
          .add(new JsonObject()
            .put("uri", OkapiMock.uri1)
            .put("publicNote", OkapiMock.uri1))
          );
     CRUtil.putItemUpdate(newItem, okapiHeaders, vertx.getOrCreateContext())
         .setHandler(putRes -> {
       if(putRes.failed()) {
         context.fail(putRes.cause());
       } else {
         CRUtil.lookupItemHoldingsInstanceByItemId(itemId, okapiHeaders,
             vertx.getOrCreateContext()).setHandler(getRes -> {
           if(getRes.failed()) {
             context.fail(getRes.cause());
           } else {
             async.complete();
           }
         });
       }
     });
   }

   @Test
   public void testGetTermsByDate(TestContext context) {
     Async async = context.async();
     String url = baseUrl + "/terms?query=endDate+>+2020-01-01T00:00:00Z";
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get terms by term date").setHandler(res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         try {
           JsonArray termArray = res.result().getJson().getJsonArray("terms");
           context.assertEquals(termArray.size(), 1);
           async.complete();
         } catch(Exception e) {
           context.fail(e);
         }
       }
     });
   }

   @Test
   public void testGetCourselistingsByTermDate(TestContext context) {
     Async async = context.async();
     String url = baseUrl + "/courselistings?query=term.endDate+>+2020-01-01T00:00:00Z";
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courselistings by term date").setHandler(res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         try {
           JsonArray courselistingArray = res.result().getJson().getJsonArray("courseListings");
           context.assertEquals(courselistingArray.size(), 1);
           async.complete();
         } catch(Exception e) {
           context.fail(e);
         }
       }
     });
   }

   @Test
   public void testSearchCoursesByCourseListingExternalId(TestContext context) {
     Async async = context.async();
     String url = baseUrl + "/courses?query=courseListing.externalId==" + EXTERNAL_ID_1;
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courses by courselisting external id").setHandler(res -> {
      if(res.failed()) {
    context.fail(res.cause());
      } else {
        try {
          JsonArray courseArray = res.result().getJson().getJsonArray("courses");
          context.assertEquals(courseArray.size(), 2);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void testGetCourselistingsByInstructorName(TestContext context) {
     Async async = context.async();
     String url = baseUrl + "/courselistings?query=instructor.name=Boffins";
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courselistings by instructor name").setHandler(res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         try {
           JsonArray courselistingArray = res.result().getJson().getJsonArray("courseListings");
           context.assertEquals(courselistingArray.size(), 1);
           async.complete();
         } catch(Exception e) {
           context.fail(e);
         }
       }
     });
  }

  @Test
  public void testGetCourselistingsByInstructorObject(TestContext context) {
     Async async = context.async();
     String url = baseUrl + "/courselistings?query=instructorObjects=Boffins";
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courselistings by instructor name").setHandler(res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         try {
           JsonArray courselistingArray = res.result().getJson().getJsonArray("courseListings");
           context.assertEquals(courselistingArray.size(), 1);
           async.complete();
         } catch(Exception e) {
           context.fail(e);
         }
       }
     });
  }



  @Test
  public void testSearchCoursesByCourseListingInstructorName(TestContext context) {
     Async async = context.async();
     String url = baseUrl + "/courses?query=courseListing.instructorObjects=Boffins";
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courses by courselisting instructor name").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonArray courseArray = res.result().getJson().getJsonArray("courses");
          context.assertEquals(courseArray.size(), 2);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void testSearchCoursesByTermId(TestContext context) {
    Async async = context.async();
    String url = baseUrl + "/courses?query=courseListing.termId=" + TERM_2_ID;
    TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courses by term id").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonArray courseArray = res.result().getJson().getJsonArray("courses");
          context.assertEquals(courseArray.size(), 1);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void testSearchReservesByTermId(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reserveJson = new JsonObject()
        .put("id", reserveId)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyRightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_3_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    String url = baseUrl + "/courselistings/" + COURSE_LISTING_3_ID + "/reserves";
    TestUtil.doRequest(vertx, url, POST, standardHeaders, reserveJson.encode(),
        201, "Post Reserve to Courselisting 3")
    .compose(f -> {
      String getCLUrl = baseUrl + "/courselistings/" + COURSE_LISTING_3_ID;
      Future<WrappedResponse> future = Future.future();
      TestUtil.doRequest(vertx, getCLUrl, GET, standardHeaders, null, 200,
          "Get CourseListing 3").setHandler(res -> {
        if(res.failed()) {
          future.fail(res.cause());
        }
        else {
          try {
            JsonObject CLJson = res.result().getJson();
            assertEquals(CLJson.getString("termId"), TERM_2_ID);
            future.complete(res.result());
          } catch(Exception e) {
            future.fail(e);
          }
        }
      });
      return future;
    })
    .compose(f -> {
      String getUrl = baseUrl + "/reserves?query=courseListing.termId=" + TERM_2_ID;
      return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 200,
         "Get reserves by term id");
    }).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonArray reserveArray = res.result().getJson().getJsonArray("reserves");
          context.assertEquals(reserveArray.size(), 1);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void testSearchReservesByProcessingStatus(TestContext context) {
    Async async = context.async();
    String reserve1Id = UUID.randomUUID().toString();
    String reserve2Id = UUID.randomUUID().toString();
    String reserve3Id = UUID.randomUUID().toString();
    JsonObject reserve1Json = new JsonObject()
        .put("id", reserve1Id)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve2Json = new JsonObject()
        .put("id", reserve2Id)
        .put("itemId", OkapiMock.item2Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve3Json = new JsonObject()
        .put("id", reserve3Id)
        .put("itemId", OkapiMock.item3Id)
        .put("processingStatusId", PROCESSING_STATUS_2_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    String url = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves";
    TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve1Json.encode(),
        201, "Post Reserve 1 to Courselisting 1")
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve2Json.encode(),
        201, "Post Reserve 2 to Courselisting 1");
    })
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve3Json.encode(),
        201, "Post Reserve 3 to Courselisting 1");
    })
   .compose( f -> {
     String getUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves" +
         "?query=processingStatus.name==frombulating";
      return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 200,
          "Get Reserves by Processing Status");
   }).setHandler(res -> {
     if(res.failed()) {
       context.fail(res.cause());
     } else {
       try {
         context.assertEquals(res.result().getJson().getJsonArray("reserves").size(), 1);
         async.complete();
       } catch(Exception e) {
         context.fail(e);
       }
     }
   });
  }


  @Test
  public void testGetExpandedReservesFromCourseListing(TestContext context) {
    Async async = context.async();
    String reserve1Id = UUID.randomUUID().toString();
    String reserve2Id = UUID.randomUUID().toString();
    String reserve3Id = UUID.randomUUID().toString();
    JsonObject reserve1Json = new JsonObject()
        .put("id", reserve1Id)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve2Json = new JsonObject()
        .put("id", reserve2Id)
        .put("itemId", OkapiMock.item2Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve3Json = new JsonObject()
        .put("id", reserve3Id)
        .put("itemId", OkapiMock.item3Id)
        .put("processingStatusId", PROCESSING_STATUS_2_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    String url = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves";
    TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve1Json.encode(),
        201, "Post Reserve 1 to Courselisting 1")
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve2Json.encode(),
        201, "Post Reserve 2 to Courselisting 1");
    })
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve3Json.encode(),
        201, "Post Reserve 3 to Courselisting 1");
    })
   .compose( f -> {
     String getUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves?expand=*";
      return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 200,
          "Get Reserves");
   }).setHandler(res -> {
     if(res.failed()) {
       context.fail(res.cause());
     } else {
       try {
         JsonArray reserveArray = res.result().getJson().getJsonArray("reserves");
         context.assertEquals(3, reserveArray.size());
         for(Object ob : reserveArray) {
           JsonObject reserve = (JsonObject)ob;
           context.assertNotNull(reserve.getJsonObject("processingStatusObject"));
           context.assertNotNull(reserve.getJsonObject("temporaryLoanTypeObject"));
           JsonObject copiedItem = reserve.getJsonObject("copiedItem");
           context.assertNotNull(copiedItem.getJsonObject("temporaryLocationObject"));
           context.assertNotNull(copiedItem.getJsonObject("permanentLocationObject"));
         }
         async.complete();
       } catch(Exception e) {
         context.fail(e);
       }
     }
   });
  }

@Test
  public void testGetExpandedReserves(TestContext context) {
    Async async = context.async();
    String reserve1Id = UUID.randomUUID().toString();
    String reserve2Id = UUID.randomUUID().toString();
    String reserve3Id = UUID.randomUUID().toString();
    JsonObject reserve1Json = new JsonObject()
        .put("id", reserve1Id)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve2Json = new JsonObject()
        .put("id", reserve2Id)
        .put("itemId", OkapiMock.item2Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve3Json = new JsonObject()
        .put("id", reserve3Id)
        .put("itemId", OkapiMock.item3Id)
        .put("processingStatusId", PROCESSING_STATUS_2_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    String url = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves";
    TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve1Json.encode(),
        201, "Post Reserve 1 to Courselisting 1")
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve2Json.encode(),
        201, "Post Reserve 2 to Courselisting 1");
    })
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve3Json.encode(),
        201, "Post Reserve 3 to Courselisting 1");
    })
   .compose( f -> {
     String getUrl = baseUrl + "/reserves?expand=*&query=courseListingId="+COURSE_LISTING_1_ID;
      return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 200,
          "Get Reserves");
   }).setHandler(res -> {
     if(res.failed()) {
       context.fail(res.cause());
     } else {
       try {
         JsonArray reserveArray = res.result().getJson().getJsonArray("reserves");
         context.assertEquals(3, reserveArray.size());
         for(Object ob : reserveArray) {
           JsonObject reserve = (JsonObject)ob;
           context.assertNotNull(reserve.getJsonObject("processingStatusObject"));
           context.assertNotNull(reserve.getJsonObject("temporaryLoanTypeObject"));
           JsonObject copiedItem = reserve.getJsonObject("copiedItem");
           context.assertNotNull(copiedItem.getJsonObject("temporaryLocationObject"));
           context.assertNotNull(copiedItem.getJsonObject("permanentLocationObject"));
         }
         async.complete();
       } catch(Exception e) {
         context.fail(e);
       }
     }
   });
  }
  //This test won't pass due to RMB-585
/*
  @Test
  public void testSearchReservesByCopyrightStatus(TestContext context) {
    Async async = context.async();
    String reserve1Id = UUID.randomUUID().toString();
    String reserve2Id = UUID.randomUUID().toString();
    String reserve3Id = UUID.randomUUID().toString();
    JsonObject reserve1Json = new JsonObject()
        .put("id", reserve1Id)
        .put("itemId", OkapiMock.item1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve2Json = new JsonObject()
        .put("id", reserve2Id)
        .put("itemId", OkapiMock.item2Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");

    JsonObject reserve3Json = new JsonObject()
        .put("id", reserve3Id)
        .put("itemId", OkapiMock.item2Id)
        .put("processingStatusId", PROCESSING_STATUS_2_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_2_ID))
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("startDate", "2020-01-01T00:00:00Z");
    String url = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves";
    TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve1Json.encode(),
        201, "Post Reserve 1 to Courselisting 1")
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve2Json.encode(),
        201, "Post Reserve 2 to Courselisting 1");
    })
    .compose(f -> {
      return TestUtil.doRequest(vertx, url, POST, standardHeaders, reserve3Json.encode(),
        201, "Post Reserve 3 to Courselisting 1");
    })
   .compose( f -> {
     String getUrl = baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves" +
         "?query=copyrightStatus.name==cc";
      return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 200,
          "Get Reserves by Processing Status");
   }).setHandler(res -> {
     if(res.failed()) {
       context.fail(res.cause());
     } else {
       try {
         context.assertEquals(res.result().getJson().getJsonArray("reserve").size(), 2);
         async.complete();
       } catch(Exception e) {
         context.fail(e);
       }
     }
   });
  }
  */

   @Test
   public void testPutEmptyLocationIdToCourseListing(TestContext context) {
     Async async = context.async();
     String courseListingId = UUID.randomUUID().toString();
     String courseId = UUID.randomUUID().toString();
     JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString())
        .put("locationId", OkapiMock.location1Id);
     JsonObject courseJson = new JsonObject()
        .put("id", courseId)
        .put("departmentId", DEPARTMENT_1_ID)
        .put("courseListingId", courseListingId)
        .put("name", "Woodworking 101");
     TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, standardHeaders,
        courseListingJson.encode(), 201, "Post Course Listing").setHandler(postRes -> {
       if(postRes.failed()) {
         context.fail(postRes.cause());
       } else {
         TestUtil.doRequest(vertx, baseUrl + "/courses", POST, standardHeaders,
             courseJson.encode(), 201, "Post Course").setHandler(postCourseRes -> {
          TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + courseListingId,
              GET, standardHeaders, null, 200, "Get new course listing").setHandler(getRes -> {
            if(getRes.failed()) {
              context.fail(getRes.cause());
            } else {
              try {
                JsonObject json = getRes.result().getJson();
                context.assertTrue(json.containsKey("locationObject"));
                context.assertEquals(OkapiMock.location1Id,
                    json.getJsonObject("locationObject").getString("id"));
                courseListingJson.putNull("locationId");
                TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + courseListingId,
                    PUT, acceptTextHeaders, courseListingJson.encode(), 204,
                    "Get new course listing").setHandler(putRes -> {
                 if(putRes.failed()) {
                   context.fail(putRes.cause());
                 } else {
                   TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + courseListingId,
                       GET, standardHeaders, null, 200, "Get new course listing")
                       .setHandler(getRes2 -> {
                     if(getRes2.failed()) {
                       context.fail(getRes2.cause());
                     } else {
                       try {
                         context.assertNull(getRes2.result().getJson()
                             .getJsonObject("locationObject"));
                         TestUtil.doRequest(vertx, baseUrl + "/courses/" + courseId,
                             GET, standardHeaders, null, 200, "get course")
                             .setHandler(getCourseRes -> {
                           if(getCourseRes.failed()) {
                             context.fail(getCourseRes.cause());
                           } else {
                             try {
                               JsonObject courseResultJson = getCourseRes.result().getJson();
                               JsonObject courseListingResultJson = courseResultJson.getJsonObject("courseListingObject");
                               context.assertTrue(courseListingResultJson.getJsonObject("locationObject") == null ||
                                   courseListingResultJson.getJsonObject("locationObject").isEmpty());
                               async.complete();
                             } catch(Exception e) {
                               context.fail(e);
                             }
                           }
                         });
                       } catch(Exception e) {
                         context.fail(e);
                       }
                     }
                   });
                 }
                });
              } catch(Exception e) {
                context.fail(e);
              }
            }
          });
        });
       }
     });

   }

   @Test
   public void testGetInstructorsForCourseListing(TestContext context) {
     Async async = context.async();
     CRUtil.lookupInstructorsForCourseListing(COURSE_LISTING_1_ID, okapiHeaders,
         vertx.getOrCreateContext()).setHandler(res -> {
       if(res.failed()) {
         context.fail(res.cause());
       } else {
         List<Instructor> instructorList = res.result();
         context.assertTrue(instructorList.size() == 2);
         async.complete();
       }
     });
   }

 //sortby tests suspended
 /*
 @Test
  public void testSortCoursesByCourseListingExternalIdAscending(TestContext context)
      throws UnsupportedEncodingException {
     Async async = context.async();
     String query = "(cql.allRecords=1) sortby courseListing.externalId/sort.ascending";
     String url = baseUrl + "/courses?query=" + URLEncoder.encode(query, "utf8");
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courses by courselisting instructor name").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonArray courseArray = res.result().getJson().getJsonArray("courses");
          context.assertEquals(courseArray.size(), 4);
          context.assertEquals(courseArray.getJsonObject(0).getString("courseListingId"),
              COURSE_LISTING_1_ID);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void testSortCoursesByCourseListingExternalIdDescending(TestContext context)
      throws UnsupportedEncodingException {
     Async async = context.async();
     String query = "(cql.allRecords=1) sortby courseListing.externalId/sort.descending";
     String url = baseUrl + "/courses?query=" + URLEncoder.encode(query, "utf8");
     TestUtil.doRequest(vertx, url, GET, standardHeaders, null, 200,
         "Get courses by courselisting instructor name").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonArray courseArray = res.result().getJson().getJsonArray("courses");
          context.assertEquals(courseArray.size(), 4);
          context.assertEquals(courseArray.getJsonObject(0).getString("courseListingId"),
              COURSE_LISTING_2_ID);
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }
  */

  @Test
  public void testCreateCourseListingAndReserveByBarcode(TestContext context) {
    Async async = context.async();
    String courseListingId = UUID.randomUUID().toString();
    JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", "1234");
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, null,
        courseListingJson.encode(), 201, "Post Course Listing").setHandler(postCLRes -> {
      if(postCLRes.failed()) {
        context.fail(postCLRes.cause());
      } else {
        String reserveId = UUID.randomUUID().toString();
        JsonObject reservePostJson = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
        )
        .put("copiedItem", new JsonObject()
          .put("barcode", OkapiMock.barcode1)
        );
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
            "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
            "Post Course Reserve").setHandler(postReserveRes -> {
          if(postReserveRes.failed()) {
            context.fail(postReserveRes.cause());
          } else {
            reservePostJson.put("processingStatusId", PROCESSING_STATUS_1_ID);
            TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
              "/reserves/" + reserveId, PUT, standardHeaders, reservePostJson.encode(), 204,
              "PUT Course Reserve").setHandler(putReserveRes -> {
                if(putReserveRes.failed()) {
                  context.fail(putReserveRes.cause());
                } else {
                  TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
                    "/reserves/" + reserveId, GET, standardHeaders, reservePostJson.encode(), 200,
                    "GET Course Reserve").setHandler(getReserveRes -> {
                    if(getReserveRes.failed()) {
                      context.fail(getReserveRes.cause());
                    } else {
                      try {
                        JsonObject reserveJson = getReserveRes.result().getJson();
                        context.assertEquals(reserveJson.getString("processingStatusId"), PROCESSING_STATUS_1_ID);
                        context.assertEquals(reserveJson.getJsonObject("processingStatusObject").getString("id"),
                            PROCESSING_STATUS_1_ID);
                        async.complete();
                      } catch(Exception e) {
                        context.fail(e);
                      }
                    }
                  });
                }
            });
          }
        });
      }
    });
  }

  @Test
  public void testInstructorsForModifiedCourseListing(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID,
        GET, standardHeaders, null, 200, "Get course listing").compose(f -> {
      context.assertTrue(f.getJson().containsKey("instructorObjects"));
      context.assertTrue(f.getJson().getJsonArray("instructorObjects").size() > 1);
      JsonObject clJson = f.getJson();
      clJson.put("termId", TERM_2_ID);

      return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID,
        PUT, acceptTextHeaders, clJson.encode(), 204, "Modify course listing");

      //return Future.succeededFuture(f);
    }).compose(f -> {
      return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID,
        GET, acceptTextHeaders, null, 200, "Get course listing again");
    }).compose( f-> {
      context.assertEquals(f.getJson().getString("termId"), TERM_2_ID);
      context.assertTrue(f.getJson().containsKey("instructorObjects"));
      context.assertTrue(f.getJson().getJsonArray("instructorObjects").size() > 1);
      return Future.succeededFuture(f);
    }).setHandler(res -> {
      if(res.succeeded()) {
        async.complete();
      } else {
        context.fail(res.cause());
      }
    });
  }

  @Test
  public void testAddReserveByBarcodeTwice(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson = new JsonObject()
    .put("id", reserveId)
    .put("courseListingId", COURSE_LISTING_1_ID)
    .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
    .put("copyrightTracking", new JsonObject()
      .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
    )
    .put("copiedItem", new JsonObject()
      .put("barcode", OkapiMock.barcode1)
    );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
      "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
      "Post Course Reserve").setHandler(postReserveRes -> {
      if(postReserveRes.failed()) {
        context.fail(postReserveRes.cause());
      } else {
        reservePostJson.put("id", UUID.randomUUID().toString());
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
          "/reserves", POST, standardHeaders, reservePostJson.encode(), 422,
          "Post Course Reserve").setHandler(repostReserveRes -> {
          if(repostReserveRes.failed()) {
            context.fail(repostReserveRes.cause());
          } else {
            async.complete();
          }
        });
      }
    });
  }


  @Test
  public void testAddReserveWithTemporaryLoanType(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson = new JsonObject()
    .put("id", reserveId)
    .put("courseListingId", COURSE_LISTING_1_ID)
    .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
    .put("copyrightTracking", new JsonObject()
      .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
    )
    .put("copiedItem", new JsonObject()
      .put("barcode", OkapiMock.barcode1)
    );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
      "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
      "Post Course Reserve").compose(f -> {
      return TestUtil.doRequest(vertx,
          baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/reserves/" + reserveId,
          GET, standardHeaders, null, 200, "Get newly created reserve");
    }).compose(f -> {
      String itemId = f.getJson().getString("itemId");
      return TestUtil.doOkapiRequest(vertx, "/item-storage/items/" + itemId,
          GET, okapiHeaders, null, null, 200, "Get item record");
    }).compose(f -> {
      context.assertEquals(OkapiMock.loanType1Id,
            f.getJson().getString("temporaryLoanTypeId"));
      reservePostJson.put("temporaryLoanTypeId", OkapiMock.loanType2Id);
      return TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
          "/reserves/" + reserveId, PUT, standardHeaders, reservePostJson.encode(),
          204, "Update reserve record");
    }).compose( f -> {
      return TestUtil.doOkapiRequest(vertx, "/item-storage/items/" + OkapiMock.item1Id,
          GET, okapiHeaders, null, null, 200, "Get item record");
    }).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        context.assertEquals(OkapiMock.loanType2Id,
            res.result().getJson().getString("temporaryLoanTypeId"));
        async.complete();
      }
    });

  }

   @Test
  public void testAddSameReserveToDifferentListing(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    String reserveId2 = UUID.randomUUID().toString();
    JsonObject reservePostJson = new JsonObject()
    .put("id", reserveId)
    .put("courseListingId", COURSE_LISTING_1_ID)
    .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
    .put("copyrightTracking", new JsonObject()
      .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
    )
    .put("copiedItem", new JsonObject()
      .put("barcode", OkapiMock.barcode1)
    );
    JsonObject reservePostJson2 = new JsonObject()
    .put("id", reserveId2)
    .put("courseListingId", COURSE_LISTING_2_ID)
    .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
    .put("copyrightTracking", new JsonObject()
      .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID)
    )
    .put("copiedItem", new JsonObject()
      .put("barcode", OkapiMock.barcode1)
    );
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
      "/reserves", POST, standardHeaders, reservePostJson.encode(), 201,
      "Post Course Reserve").setHandler(postReserveRes -> {
      if(postReserveRes.failed()) {
        context.fail(postReserveRes.cause());
      } else {
        reservePostJson.put("id", UUID.randomUUID().toString());
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_2_ID +
          "/reserves", POST, standardHeaders, reservePostJson2.encode(), 201,
          "Post Course Reserve").setHandler(repostReserveRes -> {
          if(repostReserveRes.failed()) {
            context.fail(repostReserveRes.cause());
          } else {
            async.complete();
          }
        });
      }
    });
  }

  @Test
  public void testResetItemBadId(TestContext context) {
    Async async = context.async();
    new CourseAPI().resetItemTemporaryLocation(UUID.randomUUID().toString(),
        okapiHeaders, vertx.getOrCreateContext()).setHandler(res -> {
      if(res.succeeded()) {
        context.fail("Expected failure");
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testDeleteReserveWithDeletedItem(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson1 = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doOkapiRequest(vertx, "/item-storage/items/" +OkapiMock.item1Id,
            DELETE, okapiHeaders, null, null, 204, "Delete Item 1")
        //CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/item-storage/items/")
        //    + OkapiMock.item1Id, DELETE, null, null, 204)
            .setHandler(deleteRes -> {
          if(deleteRes.failed()) {
            context.fail(deleteRes.cause());
          } else {
            TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
                "/reserves/" + reserveId, DELETE, acceptTextHeaders, null, 204,
                "Delete Course Reserve").setHandler(deleteReserveRes -> {
              if(deleteReserveRes.failed()) {
                context.fail(deleteReserveRes.cause());
              } else {
                async.complete();
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void testFallBackForCopiedReserveValues(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson1 = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item3Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, GET, standardHeaders, null, 200,
        "Post Course Reserve").setHandler(getRes -> {
          if(getRes.failed()) {
            context.fail(getRes.cause());
          } else {
            try {
              JsonObject reserveJson = getRes.result().getJson();
              JsonObject copiedJson = reserveJson.getJsonObject("copiedItem");
              context.assertEquals(copiedJson.getString("uri"), OkapiMock.uri2);
              context.assertEquals(copiedJson.getString("url"), OkapiMock.note2);
              async.complete();
            } catch(Exception e) {
              context.fail(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void testCourseListingFallBackForLocation(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson1 = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_3_ID)
        .put("itemId", OkapiMock.item3Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_3_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, GET, standardHeaders, null, 200,
        "Post Course Reserve").setHandler(getRes -> {
          if(getRes.failed()) {
            context.fail(getRes.cause());
          } else {
            try {
              JsonObject reserveJson = getRes.result().getJson();
              JsonObject copiedJson = reserveJson.getJsonObject("copiedItem");
              context.assertEquals(OkapiMock.location2Id, copiedJson.getString("temporaryLocationId"));
              CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/item-storage/items/" + reserveJson.getString("itemId"),
                  GET, null, null, 200).setHandler(itemRes -> {
                if(itemRes.failed()) {
                  context.fail(itemRes.cause());
                } else {
                  try {
                    JsonObject itemJson = itemRes.result();
                    context.assertEquals(OkapiMock.location2Id, itemJson.getString("temporaryLocationId"));
                    async.complete();
                  } catch(Exception e) {
                    context.fail(e);
                  }
                }
              });
            } catch(Exception e) {
              context.fail(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void testCourseListingFallBackForLocationFromBarcode(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson1 = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_3_ID)
        .put("copiedItem", new JsonObject().put("barcode", OkapiMock.barcode4))
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_3_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, GET, standardHeaders, null, 200,
        "Post Course Reserve").setHandler(getRes -> {
          if(getRes.failed()) {
            context.fail(getRes.cause());
          } else {
            try {
              JsonObject reserveJson = getRes.result().getJson();
              JsonObject copiedJson = reserveJson.getJsonObject("copiedItem");
              context.assertEquals(OkapiMock.location2Id, copiedJson.getString("temporaryLocationId"));
              CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/item-storage/items/" + reserveJson.getString("itemId"),
                  GET, null, null, 200).setHandler(itemRes -> {
                if(itemRes.failed()) {
                  context.fail(itemRes.cause());
                } else {
                  try {
                    JsonObject itemJson = itemRes.result();
                    context.assertEquals(OkapiMock.location2Id, itemJson.getString("temporaryLocationId"));
                    async.complete();
                  } catch(Exception e) {
                    context.fail(e);
                  }
                }
              });
            } catch(Exception e) {
              context.fail(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void testReserveCallNumberFromItem(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson1 = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item1Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, GET, standardHeaders, null, 200,
        "Post Course Reserve").setHandler(getRes -> {
          if(getRes.failed()) {
            context.fail(getRes.cause());
          } else {
            try {
              JsonObject reserveJson = getRes.result().getJson();
              JsonObject copiedJson = reserveJson.getJsonObject("copiedItem");
              context.assertEquals(copiedJson.getString("callNumber"),
                  CRUtil.makeCallNumber(OkapiMock.callNumberPrefix1,
                  OkapiMock.callNumber1, OkapiMock.callNumberSuffix1));
              async.complete();
            } catch(Exception e) {
              context.fail(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void testReserveCallNumberFromHoldings(TestContext context) {
    Async async = context.async();
    String reserveId = UUID.randomUUID().toString();
    JsonObject reservePostJson1 = new JsonObject()
        .put("id", reserveId)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("itemId", OkapiMock.item3Id)
        .put("temporaryLoanTypeId", OkapiMock.loanType1Id)
        .put("processingStatusId", PROCESSING_STATUS_1_ID)
        .put("copyrightTracking", new JsonObject()
          .put("copyrightStatusId", COPYRIGHT_STATUS_1_ID));
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves", POST, standardHeaders, reservePostJson1.encode(), 201,
        "Post Course Reserve").setHandler(postRes -> {
      if(postRes.failed()) {
        context.fail(postRes.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/reserves/" + reserveId, GET, standardHeaders, null, 200,
        "Post Course Reserve").setHandler(getRes -> {
          if(getRes.failed()) {
            context.fail(getRes.cause());
          } else {
            try {
              JsonObject reserveJson = getRes.result().getJson();
              JsonObject copiedJson = reserveJson.getJsonObject("copiedItem");
              context.assertEquals(copiedJson.getString("callNumber"),
                  CRUtil.makeCallNumber(OkapiMock.callNumberPrefix2,
                  OkapiMock.callNumber2, OkapiMock.callNumberSuffix2));
              async.complete();
            } catch(Exception e) {
              context.fail(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void loadAndRetrieveCourseListingWithScrubbedFields(TestContext context) {
    Async async = context.async();
    String courseListingId = UUID.randomUUID().toString();
    JsonObject courseListingJson = new JsonObject()
        .put("id", courseListingId)
        .put("termId", TERM_1_ID)
        .put("termObject", new JsonObject().put("id", TERM_2_ID).put("name", "whatever")
           .put("startDate", "2020-01-01").put("endDate","2000-01-01"))
        .put("externalId", UUID.randomUUID().toString())
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("courseTypeObject", new JsonObject().put("id", COURSE_TYPE_2_ID).put("name","whatever"))
        .put("locationId", OkapiMock.location1Id)
        .put("locationObject", new JsonObject().put("id", OkapiMock.location2Id))
        .put("instructorObjects", new JsonArray()
            .add(new JsonObject().put("id", INSTRUCTOR_1_ID).put("name", "whatever")
                .put("courseListingId", courseListingId)));

    Future<WrappedResponse> clFuture = TestUtil.doRequest(vertx, baseUrl + "/courselistings",
        POST, standardHeaders, courseListingJson.encode(), 201, "Post CourseListing With Location")
          .compose(res -> {
              JsonObject courseJson = new JsonObject()
                  .put("id", UUID.randomUUID().toString())
                  .put("departmentId", DEPARTMENT_1_ID)
                  .put("courseListingId", courseListingId)
                  .put("name", "Bogus Test Course");
              return TestUtil.doRequest(vertx, baseUrl + "/courses", POST, standardHeaders,
                  courseJson.encode(), 201, "Post Course with new Course Listing");
        }).compose(res -> {
          String courseId = res.getJson().getString("id");
          return TestUtil.doRequest(vertx, baseUrl + "/courses/" + courseId,
              GET, standardHeaders, null, 200, "Get newly created Course");
        }).setHandler(res -> {
          if(res.failed()) {
          context.fail(res.cause());
          } else {
            JsonObject resultJson = res.result().getJson();
            JsonObject clJson = resultJson.getJsonObject("courseListingObject");
            if(clJson == null) {
              context.fail("No courseListingObject found in result");
            } else if(!clJson.containsKey("locationObject")) {
              context.fail("No location object in result: " + resultJson.encode());
            } else if(clJson.getJsonObject("locationObject") == null) {
              context.fail("Null location object result");
            } else if(!clJson.getJsonObject("locationObject")
                .getString("id").equals(OkapiMock.location1Id)) {
              context.fail("Returned id for locationObject does not match");
            } else {
              async.complete();
            }
          }
        });
  }

  @Test
  public void testLookupUniqueItemIdCourseListingFail(TestContext context) {
    Async async = context.async();
    new CourseAPIFail().checkUniqueReserveForListing(UUID.randomUUID().toString(),
        UUID.randomUUID().toString(), okapiHeaders, vertx.getOrCreateContext())
        .setHandler(res -> {
      if(res.failed()) {
        async.complete();
      } else {
        context.fail("Expected failed result");
      }
    });
  }






  /* UTILITY METHODS */

  private Future<Void> testPostGetPutDelete(JsonObject originalJson, JsonObject modifiedJson,
      String postUrl, String getUrl, String putUrl, String deleteUrl, String deleteAllUrl) {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, postUrl, POST, standardHeaders, originalJson.encode(), 201,
        "Post to " + postUrl)
        .compose( f -> {
          return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 200,
              "Get from " + getUrl);
        })
        .compose(f -> {
          String getManyUrl = postUrl;
          return TestUtil.doRequest(vertx, getManyUrl, GET, standardHeaders, null, 200,
              "Get from " + getManyUrl);
        })
        .compose(f -> {
          String getManyQueryUrl = postUrl + "?query=cql.allRecords=1";
          return TestUtil.doRequest(vertx, postUrl, GET, standardHeaders, null, 200,
              "Get from " + getManyQueryUrl);
        })
        .compose(f -> {
          return TestUtil.doRequest(vertx, putUrl, PUT, acceptTextHeaders,
              modifiedJson.encode(), 204, "Put to " + putUrl);
        })
        .compose(f -> {
          return TestUtil.doRequest(vertx, deleteUrl, DELETE, acceptTextHeaders, null,
              204, "Delete at " + deleteUrl);
        })

        .compose(f -> {
          return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 404,
              "Get from " + getUrl + " after delete");
        })

        .compose(f -> {
          return TestUtil.doRequest(vertx, postUrl, POST, standardHeaders,
              originalJson.encode(), 201, "Post to " + postUrl);
        })
        .compose(f -> {
          return TestUtil.doRequest(vertx, deleteAllUrl, DELETE, acceptTextHeaders, null,
              204, "Delete all at " + deleteAllUrl);
        })

        .compose(f -> {
          return TestUtil.doRequest(vertx, getUrl, GET, standardHeaders, null, 404,
              "Get from " + getUrl + " after delete all");
        })

        .setHandler(res -> {
          if(res.failed()) {
            future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseListing1Instructor1() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", INSTRUCTOR_1_ID)
        .put("name", "Blaufarb")
        .put("userId", OkapiMock.user2Id)
        .put("courseListingId", COURSE_LISTING_1_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/instructors", POST, standardHeaders,
        departmentJson.encode(), 201, "Post Instructor 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseListing1Instructor2() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", INSTRUCTOR_2_ID)
        .put("name", "Kregley")
        .put("userId", OkapiMock.user3Id)
        .put("courseListingId", COURSE_LISTING_1_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        +"/instructors", POST, standardHeaders,
        departmentJson.encode(), 201, "Post Instructor 2").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

    private Future<Void> loadCourseListing2Instructor3() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", INSTRUCTOR_3_ID)
        .put("name", "Boffins")
        .put("userId", OkapiMock.user4Id)
        .put("courseListingId", COURSE_LISTING_2_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_2_ID
        +"/instructors", POST, standardHeaders,
        departmentJson.encode(), 201, "Post Instructor 3").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }


  private Future<Void> loadDepartment1() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", DEPARTMENT_1_ID)
        .put("name", "History");
    TestUtil.doRequest(vertx, baseUrl + "/departments", POST, null,
        departmentJson.encode(), 201, "Post Department 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadDepartment2() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", DEPARTMENT_2_ID)
        .put("name", "Engineering");
    TestUtil.doRequest(vertx, baseUrl + "/departments", POST, null,
        departmentJson.encode(), 201, "Post Department 2").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }


  private Future<Void> loadTerm1() {
    Future<Void> future = Future.future();
    DateTime startDate = new DateTime(2019, 6, 15, 0, 0);
    DateTime endDate = new DateTime(2019, 12, 15, 0, 0);
    JsonObject termJson = new JsonObject()
        .put("id", TERM_1_ID)
        .put("name", "Term 1")
        .put("startDate", startDate.toString(ISODateTimeFormat.dateTime()))
        .put("endDate", endDate.toString(ISODateTimeFormat.dateTime()));
    TestUtil.doRequest(vertx, baseUrl + "/terms", POST, null,
        termJson.encode(), 201, "Post Term 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadTerm2() {
    Future<Void> future = Future.future();
    DateTime startDate = new DateTime(2019, 11, 5, 0, 0);
    DateTime endDate = new DateTime(2020, 01, 15, 0, 0);
    JsonObject termJson = new JsonObject()
        .put("id", TERM_2_ID)
        .put("name", "Term 2")
        .put("startDate", startDate.toString(ISODateTimeFormat.dateTime()))
        .put("endDate", endDate.toString(ISODateTimeFormat.dateTime()));
    TestUtil.doRequest(vertx, baseUrl + "/terms", POST, null,
        termJson.encode(), 201, "Post Term 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseType1() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", COURSE_TYPE_1_ID)
        .put("name", "Regular");
    TestUtil.doRequest(vertx, baseUrl + "/coursetypes", POST, null,
        departmentJson.encode(), 201, "Post Course Type 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseListing1() {
    Future<Void> future = Future.future();
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_1_ID)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", EXTERNAL_ID_1);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, null,
        courseListingJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseListing2() {
    Future<Void> future = Future.future();
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_2_ID)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", EXTERNAL_ID_2);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, null,
        courseListingJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseListing3() {
    Future<Void> future = Future.future();
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_3_ID)
        .put("termId", TERM_2_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("locationId", OkapiMock.location2Id)
        .put("externalId", EXTERNAL_ID_3);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, null,
        courseListingJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse1() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_1_ID)
        .put("departmentId", DEPARTMENT_1_ID)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("name", "Comp Sci 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse2() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_2_ID)
        .put("departmentId", DEPARTMENT_1_ID)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("name", "Computers for Engineers 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse3() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_3_ID)
        .put("departmentId", DEPARTMENT_1_ID)
        .put("courseListingId", COURSE_LISTING_2_ID)
        .put("name", "Data Structures 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse4() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_4_ID)
        .put("departmentId", DEPARTMENT_2_ID)
        .put("courseListingId", COURSE_LISTING_2_ID)
        .put("name", "Data Structures for Engineers 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse5() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_5_ID)
        .put("departmentId", DEPARTMENT_2_ID)
        .put("courseListingId", COURSE_LISTING_3_ID)
        .put("name", "Data Structures for Engineers 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCopyrightStatus1() {
     Future<Void> future = Future.future();
     JsonObject copyrightStatusJson = new JsonObject()
        .put("id", COPYRIGHT_STATUS_1_ID)
        .put("description", "Creative Commons")
        .put("name", "cc");
    TestUtil.doRequest(vertx, baseUrl + "/copyrightstatuses", POST, null,
        copyrightStatusJson.encode(), 201, "Post Copyright Status").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

    private Future<Void> loadCopyrightStatus2() {
     Future<Void> future = Future.future();
     JsonObject copyrightStatusJson = new JsonObject()
        .put("id", COPYRIGHT_STATUS_2_ID)
        .put("description", "Reserved")
        .put("name", "reserved");
    TestUtil.doRequest(vertx, baseUrl + "/copyrightstatuses", POST, null,
        copyrightStatusJson.encode(), 201, "Post Copyright Status").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadProcessingStatus1() {
     Future<Void> future = Future.future();
     JsonObject copyrightStatusJson = new JsonObject()
        .put("id", PROCESSING_STATUS_1_ID)
        .put("description", "Processing")
        .put("name", "processing");
    TestUtil.doRequest(vertx, baseUrl + "/processingstatuses", POST, null,
        copyrightStatusJson.encode(), 201, "Post Processing Status").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadProcessingStatus2() {
     Future<Void> future = Future.future();
     JsonObject copyrightStatusJson = new JsonObject()
        .put("id", PROCESSING_STATUS_2_ID)
        .put("description", "Frombulating")
        .put("name", "frombulating");
    TestUtil.doRequest(vertx, baseUrl + "/processingstatuses", POST, null,
        copyrightStatusJson.encode(), 201, "Post Processing Status").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourses() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courses", DELETE, null, null, 204,
        "Delete All Courses").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourseListings() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", DELETE, null, null, 204,
        "Delete All Course Listings").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteTerms() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/terms", DELETE, null, null, 204,
        "Delete All Terms").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteDepartments() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/departments", DELETE, null, null, 204,
        "Delete All Departments").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourseTypes() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/coursetypes", DELETE, null, null, 204,
        "Delete All Course Types").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourseListing1Instructors() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/"+COURSE_LISTING_1_ID+
        "/instructors", DELETE, null, null, 204,
        "Delete All Instructors for Course Listing 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourseListing2Instructors() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/"+COURSE_LISTING_2_ID+
        "/instructors", DELETE, null, null, 204,
        "Delete All Instructors For Course Listing 2").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }


  private Future<Void> deleteCopyrightStatuses() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/copyrightstatuses", DELETE, null, null, 204,
        "Delete All CopyrightStatuses").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteProcessingStatuses() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/processingstatuses", DELETE, null, null, 204,
        "Delete All ProcessingStatuses").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteReserves() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/reserves", DELETE, null, null, 204,
        "Delete All Reserves").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> resetMockOkapi() {
    Future<Void> future = Future.future();
    JsonObject payload = new JsonObject().put("reset", true);
    TestUtil.doOkapiRequest(vertx, "/reset", POST, okapiHeaders, null,
        payload.encode(), 201, "Reset Okapi").setHandler(res -> {
    //CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/reset", POST, null,
    //    payload.encode(), 201).setHandler(res -> {
      if(res.failed()) {
        future.fail(res.cause());
      } else {
        future.complete();
      }
    });
    return future;
  }

  private static Future<Void> initTenant(String tenantId, int port) {
    Promise<Void> promise = Promise.promise();
    HttpClient client = vertx.createHttpClient();
    String url = "http://localhost:" + port + "/_/tenant";
    JsonObject payload = new JsonObject()
        .put("module_to", MODULE_TO)
        .put("module_from", MODULE_FROM);
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
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Accept", "application/json, text/plain");
    request.end(payload.encode());
    return promise.future();
  }

}


class CourseAPIFail extends CourseAPI {

  public <T> Future<Results<T>> getItems(String tableName, Class<T> clazz,
      CQLWrapper cql, PostgresClient pgClient) {
    logger.info("Calling Always-Fails getItems");
    Future<Results<T>> future = Future.future();
    future = Future.failedFuture("IT ALWAYS FAILS");
    return future;
  }

  public Future<Void> deleteAllItems(String tableName, String whereClause,
      Map<String, String> okapiHeaders, Context vertxContext) {
    Future future = Future.failedFuture("IT ALWAYS FAILS");
    return future;
  }

  public Future<Void> deleteItem(String tableName, String id, Map<String,
      String> okapiHeaders, Context vertxContext) {
    Future future = Future.failedFuture("IT ALWAYS FAILS");
    return future;
  }

}

class CourseAPIWTF extends CourseAPI {

  public <T> Future<Results<T>> getItems(String tableName, Class<T> clazz,
      CQLWrapper cql, PostgresClient pgClient) {
    throw new RuntimeException("WTF");
  }

  public Future<Void> deleteAllItems(String tableName, String whereClause,
      Map<String, String> okapiHeaders, Context vertxContext) {
    throw new RuntimeException("WTF");
  }

  public Future<Void> deleteItem(String tableName, String id, Map<String,
      String> okapiHeaders, Context vertxContext) {
    throw new RuntimeException("WTF");
  }

}