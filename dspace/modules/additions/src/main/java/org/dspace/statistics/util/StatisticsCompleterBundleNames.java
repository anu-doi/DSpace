/**
 * 
 */
package org.dspace.statistics.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.Context;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.SolrLogger.ResultProcessor;

/**
 * @author Rahul Khanna
 *
 */
public class StatisticsCompleterBundleNames {

	private static Context c;
	private static boolean abort = false;
	private static boolean failFast = true;

	public static void main(String[] args) {
		try {
			initContext();
			iterateAllBitstreams();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// shutdown connection to solr instance
			if (SolrLogger.solr != null) {
				try {
					SolrLogger.solr.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// cleanly close context
			if (c != null) {
				try {
					c.complete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void initContext() throws SQLException {
		c = new Context(Context.READ_ONLY);
	}

	private static void iterateAllBitstreams() {
		Map<Integer, Set<String>> bitstreams = null;
		try {
			System.out.println("Retrieving list of bitstreams and bundles they belong to...");
			bitstreams = getBitstreamsAndBundles();
			System.out.println("Retrieved " + bitstreams.size() + "bitstream IDs");
		} catch (SQLException e) {
			System.out.println();
			System.out.println("Error retrieving bitstream list.");
			e.printStackTrace();
		}

		if (bitstreams != null) {
			for (Entry<Integer, Set<String>> iBs : bitstreams.entrySet()) {
				if (abort) {
					break;
				}

				try {
					System.out.print("Processing " + String.valueOf(iBs.getKey()) + " " + iBs.getValue() + "...");
					updateStatsForBitstream(iBs.getKey(), iBs.getValue());
					System.out.println(" done");
				} catch (SolrServerException e) {
					System.out.println();
					e.printStackTrace();
					if (failFast) {
						break;
					}
				} catch (IOException e) {
					System.out.println();
					e.printStackTrace();
					if (failFast) {
						break;
					}
				}
			}
		}
	}

	private static Map<Integer, Set<String>> getBitstreamsAndBundles() throws SQLException {
		Connection dbConnection = c.getDBConnection();
		Statement statement = dbConnection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT bs.bitstream_id, bndl.name\r\n"
				+ "FROM bitstream as bs\r\n" + "INNER JOIN bundle2bitstream b2b\r\n"
				+ "ON (bs.bitstream_id=b2b.bitstream_id)\r\n" + "INNER JOIN bundle bndl\r\n"
				+ "ON (b2b.bundle_id=bndl.bundle_id);");

		Map<Integer, Set<String>> bitstreams = new HashMap<Integer, Set<String>>();
		while (resultSet.next()) {
			int bitstreamId = resultSet.getInt("bitstream_id");
			String bundleName = resultSet.getString("name");
			if (!bitstreams.containsKey(bitstreamId)) {
				bitstreams.put(bitstreamId, new HashSet<String>(Arrays.asList(bundleName)));
			} else {
				bitstreams.get(bitstreamId).add(bundleName);
			}
		}
		return bitstreams;
	}

	private static void updateStatsForBitstream(final int bitstreamId, final Set<String> bundles)
			throws SolrServerException, IOException {
		ResultProcessor processor = new ResultProcessor() {
			@Override
			public void process(SolrDocument doc) throws IOException, SolrServerException {
				Collection<Object> solrDocFieldValues = doc.getFieldValues("bundleName");
				Set<String> solrEntryBundles;
				// store bundle names in the solr doc in solrEntryBundles
				if (solrDocFieldValues != null) {
					solrEntryBundles = new HashSet<String>(solrDocFieldValues.size());
					for (Object iValue : solrDocFieldValues) {
						solrEntryBundles.add((String) iValue);
					}
				} else {
					solrEntryBundles = new HashSet<String>(0);
				}

				boolean requiresUpdate = false;
				// compare bundle names to determine if an update's required to
				// the solr doc
				if (bundles.size() != solrEntryBundles.size()) {
					requiresUpdate = true;
				} else {
					for (String iBundle : bundles) {
						if (!solrEntryBundles.contains(iBundle)) {
							requiresUpdate = true;
							break;
						}
					}
				}

				// update the solr doc if an update's required
				if (requiresUpdate) {
					doc.removeFields("bundleName");
					doc.addField("bundleName", bundles);
					SolrInputDocument newInput = ClientUtils.toSolrInputDocument(doc);
					SolrLogger.solr.add(newInput);
				}
			}
		};

		processor.execute("id:" + bitstreamId + " AND type:0");
	}
}
