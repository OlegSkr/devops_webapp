package de.norcom.devops;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

@Path("/")
public class DevOpsService {

	@Context
	private ServletContext context;

	private static final String INDEX_NAME = "index";

	private Client client;

	private void setup() throws UnknownHostException {

		String hostname = "10.0.2.1";

		String address = context.getInitParameter("norcom.devops.elastic.address");

		System.out.println("DevOpsService.setup(\"norcom.devops.elastic.address\") = " + address);

		if (address != null && !address.isEmpty()) {
			hostname = address;
		}

		InetSocketTransportAddress transportAddress = new InetSocketTransportAddress(
			InetAddress.getByName(hostname), 9300);

		Settings settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch").build();
		client = TransportClient.builder().settings(settings).build().addTransportAddress(transportAddress);

		boolean indexExists = client.admin().indices().prepareExists(INDEX_NAME).execute().actionGet()
			.isExists();

		if (!indexExists) {
			client.admin().indices().prepareCreate(INDEX_NAME).execute().actionGet();
		}

	}

	@GET
	public Response index() throws IOException {

		if (client == null) {
			setup();
		}

		String output = "";

		output = testElastic(output);

		return Response.status(200).entity(output).build();

	}

	private String testElastic(String output) throws IOException {

		SearchResponse sr = client.prepareSearch(INDEX_NAME).setTypes("article").addFields("body")
			.setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();

		SearchHit[] results = sr.getHits().getHits();
		for (SearchHit hit : results) {

			String id = hit.getId();
			String body = hit.getFields().get("body").getValue();

			output += id + ": " + body + "<br />";
		}

		return output;
	}

}
