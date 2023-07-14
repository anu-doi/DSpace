/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.app.rest.model.hateoas.SearchEventResource;
import org.dspace.app.rest.model.hateoas.StatisticsSupportResource;
import org.dspace.app.rest.model.hateoas.ViewEventResource;
import org.dspace.app.rest.repository.SearchEventRestRepository;
import org.dspace.app.rest.repository.StatisticsRestRepository;
import org.dspace.app.rest.repository.ViewEventRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/api/" + RestAddressableModel.STATISTICS)
public class StatisticsRestController implements InitializingBean {

//	@JsonProperty("id")
//	String id = null;

	@Autowired
	private DiscoverableEndpointsService discoverableEndpointsService;

	@Autowired
	private ConverterService converter;

	@Autowired
	private StatisticsRestRepository statisticsRestRepository;

	@Autowired
	private ViewEventRestRepository viewEventRestRepository;

	@Autowired
	private SearchEventRestRepository searchEventRestRepository;

	@Autowired
	private DSpaceObjectUtils dspaceObjectUtil;

	@Autowired
	private UsageReportUtils usageReportUtils;

	
	@Override
	public void afterPropertiesSet() throws Exception {
		discoverableEndpointsService.register(this,
				Arrays.asList(Link.of("/api/" + RestAddressableModel.STATISTICS, RestAddressableModel.STATISTICS)));
	}

	@RequestMapping(method = RequestMethod.GET)
	public StatisticsSupportResource getStatisticsSupport() throws Exception {
		StatisticsSupportRest statisticsSupportRest = statisticsRestRepository.getStatisticsSupport();
		return converter.toResource(statisticsSupportRest);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/viewevents/{uuid}")
	public PagedModel<ViewEventResource> getViewEvent(@PathVariable(name = "uuid") UUID uuid) throws Exception {
		throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
	}

	@RequestMapping(method = RequestMethod.GET, value = "/searchevents/{uuid}")
	public PagedModel<SearchEventResource> getSearchEvent(@PathVariable(name = "uuid") UUID uuid) throws Exception {
		throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
	}

	@RequestMapping(method = RequestMethod.GET, value = "/viewevents")
	public PagedModel<ViewEventResource> getViewEvents() throws Exception {
		throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
	}

	@RequestMapping(method = RequestMethod.GET, value = "/searchevents")
	public PagedModel<SearchEventResource> getSearchEvents() throws Exception {
		throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
	}

	@RequestMapping(method = RequestMethod.POST, value = "/viewevents")
	public ResponseEntity<RepresentationModel<?>> postViewEvent() throws Exception {
		ViewEventResource result = converter.toResource(viewEventRestRepository.createViewEvent());
		return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/searchevents")
	public ResponseEntity<RepresentationModel<?>> postSearchEvent() throws Exception {
		SearchEventResource result = converter.toResource(searchEventRestRepository.createSearchEvent());
		return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
	}

	@GetMapping("/usagereports/exportstatistics")
	public ResponseEntity<byte[]> exportStatistics(@RequestParam(name = "uri") String uri,
			@RequestParam(name = "startdate") String startDate, @RequestParam(name = "enddate") String endDate,
			@RequestParam(name = "type") String reportType, HttpServletResponse response) throws Exception {

		RequestService requestService = new DSpace().getRequestService();
		Request currentRequest = requestService.getCurrentRequest();
		Context context = ContextUtil.obtainContext(currentRequest.getHttpServletRequest());

		UUID uuidObject = UUID.fromString((StringUtils.substringAfterLast(uri, "/")));
		DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuidObject);
		if (dso == null) {
			throw new ResourceNotFoundException("No DSO found with uuid: " + uuidObject);
		}

		String decodedUri = URLDecoder.decode(uri, StandardCharsets.UTF_8.toString());

		StatisticsRestRepository srr = new StatisticsRestRepository();

		byte[] output = (srr.exportRestMethod(context, dso, decodedUri, startDate, endDate, reportType, response,
				usageReportUtils)).toByteArray();
		
		String filename = reportType+".csv";
		response.addHeader("Content-Disposition", "inline; filename=" + filename);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("text/csv"));
		headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
		
		return ResponseEntity.ok()
				.headers(headers)
				.body(output);
	}
}
