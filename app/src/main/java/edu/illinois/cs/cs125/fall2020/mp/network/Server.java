package edu.illinois.cs.cs125.fall2020.mp.network;


//import android.provider.MediaStore;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//import edu.illinois.cs.cs125.fall2020.mp.R;
import edu.illinois.cs.cs125.fall2020.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.fall2020.mp.models.Rating;
import edu.illinois.cs.cs125.fall2020.mp.models.Summary;
import java.io.IOException;
import java.net.HttpURLConnection;
//import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Development course API server.
 *
 * <p>Normally you would run this server on another machine, which the client would connect to over
 * the internet. For the sake of development, we're running the server right alongside the app on
 * the same device. However, all communication between the course API client and course API server
 * is still done using the HTTP protocol. Meaning that eventually it would be straightforward to
 * move this server to another machine where it could provide data for all course API clients.
 *
 * <p>You will need to add functionality to the server for MP1 and MP2.
 */
public final class Server extends Dispatcher {
  @SuppressWarnings({"unused", "RedundantSuppression"})
  private static final String TAG = Server.class.getSimpleName();

  private final Map<String, String> summaries = new HashMap<>();

  private MockResponse getSummary(@NonNull final String path) {
    String[] parts = path.split("/");
    if (parts.length != 2) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    String summary = summaries.get(parts[0] + "_" + parts[1]);
    if (summary == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(summary);
  }

  private String theString = "";
  private MockResponse testPost(@NonNull final RecordedRequest request) {
    if (request.getMethod().equals("GET")) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(theString);
    } else if (request.getMethod().equals("POST")) {
      theString = request.getBody().readUtf8();
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP).setHeader(
              "Location", "/test/"
      );
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
  }


  private final Map<Summary, Map<String, Rating>> ratings = new HashMap<>();
  private Map<String, Rating> innerMap = new HashMap<>();
  private MockResponse getRating(@NonNull final RecordedRequest request) {
    if (request.getMethod().trim().equals("GET")) {
      String path = request.getPath();
      String[] parts = path.split("/");
      final int five = 5;
      final int four = 4;
      final int six = 6;
      final int uuidLength = 36;
      if (!(parts[1].equals("rating"))) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      }
      if (parts.length != six) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }

      if (!(parts[five].contains("client"))) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }
      Summary newSummary = new Summary(parts[2], parts[3], parts[four], parts[five].substring(0, 3), "");
      String uuid = parts[five].substring((parts[five].lastIndexOf("=") + 1)).trim();

      if (uuid.length() != uuidLength) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }
      if ((courses.containsKey(newSummary) == false)) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      }

      Rating rating = new Rating(uuid, Rating.NOT_RATED);
      if (ratings.containsKey(newSummary) == true) {
        if (ratings.get(newSummary).get(uuid) != null) {
          rating = ratings.get(newSummary).get(uuid);
        }
      }

      String r = new String();
      ObjectMapper m = new ObjectMapper();
      m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      try {
        r = m.writeValueAsString(rating);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(r);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
  }

  private MockResponse postRating(@NonNull final RecordedRequest request) {
    if (request.getMethod().trim().equals("POST")) {
      String path = request.getPath();
      String[] parts = path.split("/");
      String r = request.getBody().readUtf8();
      final int five = 5;
      final int four = 4;
      final int six = 6;
      final int uuidLength = 36;
      if (!(parts[1].equals("rating"))) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      }
      if (parts.length != six) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }
      if (!(parts[five].contains("client"))) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }
      Summary newSummary = new Summary(parts[2], parts[3], parts[four], parts[five].substring(0, 3), "");
      String uuid = parts[five].substring((parts[five].lastIndexOf("=") + 1)).trim();

      if (uuid.length() != uuidLength) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }

      Rating rating = new Rating();
      ObjectMapper m = new ObjectMapper();
      m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      try {
        rating = m.readValue(r, Rating.class);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

      Map<String, Rating> inner = ratings.getOrDefault(newSummary, new HashMap<>());
      inner.put(uuid, rating);
      ratings.put(newSummary, inner);

      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP).setHeader(
              "Location", path
      );
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<Summary, String> courses = new HashMap<>();

  // course/2020/fall/cs/125
  private MockResponse getCourse(@NonNull final String path) {
    final int four = 4;
    String[] parts = path.split("/");
    if (parts.length != four) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
    Summary newSummary = new Summary(parts[0], parts[1], parts[2], parts[3], "");
    String course = courses.get(newSummary);
    if (course == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(course);
  }

  @NonNull
  @Override
  public MockResponse dispatch(@NonNull final RecordedRequest request) {
    try {
      String path = request.getPath();
      if (path == null || request.getMethod() == null) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      } else if (path.equals("/") && request.getMethod().equalsIgnoreCase("HEAD")) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.startsWith("/summary/")) {
        return getSummary(path.replaceFirst("/summary/", ""));
      } else if (path.startsWith("/course/")) {
        return getCourse(path.replaceFirst("/course/", ""));
      } else if (path.equals("/test/")) {
        return testPost(request);
      } else if (request.getMethod().equals("GET")) {
        return getRating(request);
      } else if (request.getMethod().equals("POST")) {
        return postRating(request);
      }


      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    } catch (Exception e) {
      e.printStackTrace();
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
  }

  private static boolean started = false;

  /**
   * Start the server if has not already been started.
   *
   * <p>We start the server in a new thread so that it operates separately from and does not
   * interfere with the rest of the app.
   */
  public static void start() {
    if (!started) {
      new Thread(Server::new).start();
      started = true;
    }
  }

  private final ObjectMapper mapper = new ObjectMapper();

  private Server() {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    loadSummary("2020", "fall");
    loadCourses("2020", "fall");

    try {
      MockWebServer server = new MockWebServer();
      server.setDispatcher(this);
      server.start(CourseableApplication.SERVER_PORT);

      String baseUrl = server.url("").toString();
      if (!CourseableApplication.SERVER_URL.equals(baseUrl)) {
        throw new IllegalStateException("Bad server URL: " + baseUrl);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void loadSummary(@NonNull final String year, @NonNull final String semester) {
    String filename = "/" + year + "_" + semester + "_summary.json";
    String json =
        new Scanner(Server.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
    summaries.put(year + "_" + semester, json);
  }

  @SuppressWarnings("SameParameterValue")
  private void loadCourses(@NonNull final String year, @NonNull final String semester) {
    String filename = "/" + year + "_" + semester + ".json";
    String json =
        new Scanner(Server.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
    try {
      JsonNode nodes = mapper.readTree(json);
      for (Iterator<JsonNode> it = nodes.elements(); it.hasNext(); ) {
        JsonNode node = it.next();
        Summary course = mapper.readValue(node.toString(), Summary.class);
        courses.put(course, node.toPrettyString());
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
