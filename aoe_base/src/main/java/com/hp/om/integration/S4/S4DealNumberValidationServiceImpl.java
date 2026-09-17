package com.hp.om.integration.S4;

import com.hp.bcs.utils.CacheConfig;
import com.hp.service.core.LoggingDomainType;
import com.hp.service.core.Q2CLogger;
import com.hp.service.core.Q2CLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Class: S4DealNumberValidationServiceImpl
 * Author: bcak
 * Created: 6/30/2025 - as part of US-18346 : DQM Validation for S4 Deal Number
 * Description: Service implementation for validating S4 deal numbers by making HTTP requests to the S4 Addison endpoint
 * with retry logic and response handling.
 */
@Service
public class S4DealNumberValidationServiceImpl implements S4DealNumberValidationService {

    private static final Q2CLogger LOG = Q2CLoggerFactory.getLogger(S4DealNumberValidationServiceImpl.class, LoggingDomainType.OMUI);
    private static final int MAX_RETRIES = Integer.parseInt(CacheConfig.getValue("EMDM_MAX_RETRY"));
    private static final String S4_DEAL_NR_ADDISON_URL = CacheConfig.getValue("DEAL_NR_S4_ADDISON_URL");

    @Override
    public String getS4DealNumberResponse(String dealNr) {
        if (dealNr == null || dealNr.trim().isEmpty()) {
            LOG.warn("S4DealNumberValidationServiceImpl: dealNr is null or empty");
            return null;
        }
        int retries = 0;
        while (retries <= MAX_RETRIES) {
            HttpURLConnection connection = null;
            try {
                String urlStr = appendQueryParams(S4_DEAL_NR_ADDISON_URL, "dealNumber", dealNr);
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                int responseCode = connection.getResponseCode();
                LOG.debug("S4DealNumberValidationServiceImpl: HTTP GET Response Code: " + responseCode + " for URL: " + urlStr);
                if (responseCode == HttpStatus.OK.value()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else if (responseCode == HttpStatus.NOT_FOUND.value()) {
                    LOG.warn("S4DealNumberValidationServiceImpl: Resource not found (404) for dealNr: " + dealNr);
                    return null;
                } else if (responseCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    LOG.warn("S4DealNumberValidationServiceImpl: 500 Internal Server Error, retrying... attempt " + (retries + 1));
                    retries++;
                    Thread.sleep(1000);
                    continue;
                } else {
                    LOG.error("S4DealNumberValidationServiceImpl: Unexpected response code " + responseCode + " for dealNr: " + dealNr);
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                LOG.error("S4DealNumberValidationServiceImpl: Exception when calling S4 service for dealNr: " + dealNr, e);
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        LOG.error("S4DealNumberValidationServiceImpl: Max retries reached for dealNr: " + dealNr);
        return null;
    }

    private String appendQueryParams(String url, String key, String value) {
        try {
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            if (url.contains("?")) {
                return url + "&" + key + "=" + encodedValue;
            } else {
                return url + "?" + key + "=" + encodedValue;
            }
        } catch (Exception e) {
            LOG.error("S4DealNumberValidationServiceImpl: Exception encoding query param value", e);
            return url;
        }
    }
} static final String DEAL_NR_S4_ADDISON_URL = CacheConfig.getValue("DEAL_NR_S4_ADDISON_URL");

    @Override
    public String getS4DealNumberResponse(String dealNr) {
        if (dealNr == null || dealNr.isEmpty()) {
            LOG.warn("getS4DealNumberResponse called with empty dealNr");
            return null;
        }
        String urlStr = appendQueryParams(DEAL_NR_S4_ADDISON_URL, "dealNumber", dealNr);
        int retries = 0;
        while (retries <= MAX_RETRIES) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                int responseCode = connection.getResponseCode();
                LOG.debug("S4DealNumberValidationServiceImpl: HTTP GET Response Code: " + responseCode + " for URL: " + urlStr);
                if (responseCode == HttpStatus.OK.value()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else if (responseCode == HttpStatus.NOT_FOUND.value()) {
                    LOG.warn("S4DealNumberValidationServiceImpl: Resource not found (404) for dealNr: " + dealNr);
                    return null;
                } else if (responseCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    LOG.warn("S4DealNumberValidationServiceImpl: 500 Internal Server Error, retrying... attempt " + (retries + 1));
                    retries++;
                    Thread.sleep(1000);
                    continue;
                } else {
                    LOG.error("S4DealNumberValidationServiceImpl: Unexpected response code " + responseCode + " for dealNr: " + dealNr);
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                LOG.error("S4DealNumberValidationServiceImpl: Exception when calling S4 service for dealNr: " + dealNr, e);
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        LOG.error("S4DealNumberValidationServiceImpl: Max retries reached for dealNr: " + dealNr);
        return null;
    }

    private String appendQueryParams(String url, String key, String value) {
        try {
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            if (url.contains("?")) {
                return url + "&" + key + "=" + encodedValue;
            } else {
                return url + "?" + key + "=" + encodedValue;
            }
        } catch (Exception e) {
            LOG.error("S4DealNumberValidationServiceImpl: Error encoding URL parameter value", e);
            return url;
        }
    }
} for retry logic
    private static String s4DealNumberResponse = null;
    private final String dealNrS4AddisonURL = CacheConfig.getValuee("DEAL_NR_S4_ADDISON_URL");

    /**
     * @return
     */
    @Override
    public String getS4DealNumberResponse(String dealNr) {
        LOG.info("Inside S4DealNumberValidationServiceImpl.getS4DealNumberResponse method with dealNr: " + dealNr);
        String url;
        String paramKey;
        String paramValue;

        url = dealNrS4AddisonURL;
        paramKey = "dealNumber";
        paramValue = dealNr;

        //Append the query param
        String fullURL = appendQueryParams(url, paramKey, paramValue);
        LOG.info("S4DealNumberValidationServiceImpl.getS4DealNumberResponse method Request URL: " + fullURL);
        int retries = 0;
        boolean success = false;
        int maxRetries = Integer.parseInt(MAX_RETRIES);
        StringBuilder jsonOutput = new StringBuilder();
        while (retries < maxRetries && !success) {
            try {
                URL url1 = new URL(fullURL);
                HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;

                    while ((inputLine = br.readLine()) != null) {
                        jsonOutput.append(inputLine);
                    }
                    conn.disconnect();
                    br.close();
                    success = true;
                    s4DealNumberResponse = jsonOutput.toString();
                    LOG.info("DealNrS4Addison Call JSON Response: " + s4DealNumberResponse);
                } else if (responseCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    LOG.warn("SERVICE UNAVAILABLE ERROR: " + responseCode + "::" + "retrying ... retries count {} " + retries +
                            "JSON Response: " + s4DealNumberResponse);
                    Thread.sleep(1000); // wait before retrying
                    retries++;
                    LOG.debug("retries attempted :: " + retries + " status code: " + responseCode);
                } else if (responseCode == HttpStatus.NOT_FOUND.value()) {
                    LOG.error("DealNrS4Addison Call FAILED: " + responseCode);
                    break;
                } else {
                    LOG.error("Unexpected status code: " + responseCode);
                    break;
                }
            } catch (IOException | InterruptedException e) {
                e.getMessage();
            }
        }
        LOG.info("maximum retries {} reached. ", retries);
        return s4DealNumberResponse;
    }

    public String appendQueryParams(String url, String key, String value) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String separator = url.contains("?") ? "&" : "?";

        return url + separator + encodedKey + "=" + encodedValue;
    }
private static final Q2CLogger LOG = Q2CLoggerFactory.getLogger(S4DealNumberValidationServiceImpl.class, LoggingDomainType.OMUI);
    private static final String MAX_RETRIES = CacheConfig.getValuee("EMDM_MAX_RETRY"); // Using existing db value for retry logic
    private static String s4DealNumberResponse = null;
    private final String dealNrS4AddisonURL = CacheConfig.getValuee("DEAL_NR_S4_ADDISON_URL");

    /**
     * Retrieves the deal number response from the external service.
     * 
     * @param dealNr the deal number to be validated
     * @return the JSON response from the service
     */
    @Override
    public String getS4DealNumberResponse(String dealNr) {
        LOG.info("Inside S4DealNumberValidationServiceImpl.getS4DealNumberResponse method with dealNr: " + dealNr);
        String url;
        String paramKey;
        String paramValue;

        url = dealNrS4AddisonURL;
        paramKey = "dealNumber";
        paramValue = dealNr;

        // Append the query param
        String fullURL = appendQueryParams(url, paramKey, paramValue);
        LOG.info("S4DealNumberValidationServiceImpl.getS4DealNumberResponse method Request URL: " + fullURL);
        int retries = 0;
        boolean success = false;
        int maxRetries = Integer.parseInt(MAX_RETRIES);
        StringBuilder jsonOutput = new StringBuilder();

        while (retries < maxRetries && !success) {
            try {
                URL url1 = new URL(fullURL);
                HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;

                    while ((inputLine = br.readLine()) != null) {
                        jsonOutput.append(inputLine);
                    }
                    conn.disconnect();
                    br.close();
                    success = true;
                    s4DealNumberResponse = jsonOutput.toString();
                    LOG.info("DealNrS4Addison Call JSON Response: " + s4DealNumberResponse);
                } else if (responseCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    LOG.warn("SERVICE UNAVAILABLE ERROR: " + responseCode + "::" + " retrying ... retries count {} " + retries +
                            " JSON Response: " + s4DealNumberResponse);
                    Thread.sleep(1000); // wait before retrying
                    retries++;
                    LOG.debug("retries attempted :: " + retries + " status code: " + responseCode);
                } else if (responseCode == HttpStatus.NOT_FOUND.value()) {
                    LOG.error("DealNrS4Addison Call FAILED: " + responseCode);
                    break;
                } else {
                    LOG.error("Unexpected status code: " + responseCode);
                    break;
                }
            } catch (IOException | InterruptedException e) {
                LOG.error("Exception occurred: " + e.getMessage(), e);
            }
        }
        LOG.info("Maximum retries {} reached.", retries);
        return s4DealNumberResponse;
    }

    /**
     * Appends query parameters to the given URL.
     * 
     * @param url the base URL
     * @param key the query parameter key
     * @param value the query parameter value
     * @return the full URL with appended query parameters
     */
    public String appendQueryParams(String url, String key, String value) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String separator = url.contains("?") ? "&" : "?";

        return url + separator + encodedKey + "=" + encodedValue;
    }
}