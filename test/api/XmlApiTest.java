package api;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

import java.io.StringReader;

import gloo.PastesManager;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.XML;
import play.libs.XPath;
import play.mvc.Result;

public class XmlApiTest {
	@Before
	public void clearDB() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				PastesManager.deleteAll();
			}
		});
	}

	@Test
	public void saveActionRespondsBadRequestOnNonXMLContentType() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(POST, "/api/xml/"));
				assertThat(status(result)).isEqualTo(BAD_REQUEST);
			}
		});
	}

	@Test
	public void saveActionRespondsBadRequestOnNullContent() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(POST, "/api/xml/").withHeader(CONTENT_TYPE, "text/xml"));
				assertThat(status(result)).isEqualTo(BAD_REQUEST);
			}
		});
	}

	@Test
	public void saveActionRespondsOkOnValidContent() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(POST, "/api/xml/")
						.withHeader(CONTENT_TYPE, "text/xml").withXmlBody(
								new InputSource (new StringReader ( "<gloo>Probando el API xml</gloo>" ))));
				assertThat(status(result)).isEqualTo(CREATED);
				assertThat(contentType(result)).isEqualTo("text/xml");
				assertThat(contentAsString(result)).isNotEmpty();
			}
		});
	}

	@Test
	public void viewActionRespondsNotFoundOnNonExistentKey() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(GET, "/api/xml/nonExistentKey"));
				assertThat(status(result)).isEqualTo(NOT_FOUND);
			}
		});
	}

	@Test
	public void testAll () {
		running(fakeApplication(), new Runnable() {
			public void run() {
				String pasteContentToSend = "<gloo>Probando el &lt;strong&gt;API&lt;/strong&gt; xml</gloo>";
				Result result1 = route(fakeRequest(POST, "/api/xml/")
						.withHeader(CONTENT_TYPE, "text/xml").withXmlBody(
								new InputSource (new StringReader ( pasteContentToSend ))));
				Document xml1 = XML.fromString(contentAsString(result1));
				String key = XPath.selectText("//gloo", xml1);
				Result result2 = route(fakeRequest(GET, "/api/xml/"+key));
				Document xml2 = XML.fromString(contentAsString(result2));
				assertThat(xml2.getXmlEncoding()).isEqualTo("utf-8");
				assertThat(xml2.getXmlVersion()).isEqualTo("1.0");
				String content = XPath.selectText("//gloo", xml2);
				assertThat(content).isEqualTo("Probando el <strong>API</strong> xml");
			}
		});
	}

	@Test
	public void testWithWS () {
		running(testServer(3333), new Runnable() {
			public void run() {
				String pasteContentToSend = "<gloo>Probando el &lt;strong&gt;API&lt;/strong&gt; xml</gloo>";
				Promise<WS.Response> result1 = WS.url("http://localhost:3333/api/xml/").setHeader(CONTENT_TYPE, "text/xml").post(pasteContentToSend);
				Document document1 = result1.get().asXml();
				String key = XPath.selectText("//gloo", document1);
				Promise<WS.Response> result2 = WS.url("http://localhost:3333/api/xml/"+key).get();
				Document document2 = result2.get().asXml();
				String pasteContentReceived = XPath.selectText("//gloo", document2);
				assertThat(pasteContentReceived).isEqualTo("Probando el <strong>API</strong> xml");
			}
		});
	}
}
