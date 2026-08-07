package com.hpe.ocl.adminUI.dailyDocumentMail;

import com.hpe.common.log.AppLogger;
import com.hpe.common.log.AppLoggerFactory;
import com.hpe.common.log.AppLoggingDomain;
import com.hpe.common.util.CacheConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hpe.ocl.adminUI.homeSearch.service.IHomeSearchService;

import static com.hpe.ocl.scheduler.SchedulerTask.HOSTNAME;

/**
 *  US-18451 – Daily Errored Document Report Scheduler
 *
 *  This scheduler is responsible for automatically triggering Daily Errored Document Report email
 *  The CRON expression is configured in application.properties under: errored.documents.daily.mail.cron
 *  The time zone is set to UTC at 3:30pm.
 *  At the scheduled time, this method calls HomeSearchService.sendDailyDocumentListMail().
 *  The service generates an Excel file of errored documents and sends it as an email attachment to configured recipients.
 */

@Component
public class HomeSearchEmailScheduler {

    private static final AppLogger LOG =
            AppLoggerFactory.getLogger(HomeSearchEmailScheduler.class, AppLoggingDomain.OCLUI);

    private static final String ALLOWED_HOSTS_CACHE_KEY = "DailyErroredDocMailAllowedHosts";

    @Autowired
    private IHomeSearchService homeSearchService;

    @Autowired
    private CacheConfig cacheConfig;

    /**
     * US-18451
     * This method triggers the daily errored document mail job.
     * Checks if the current host is allowed to run the scheduler (amIAllowedToRun).
     * If allowed, logs and calls homeSearchService.sendDailyDocumentListMail().
     * Otherwise, logs that the scheduler was skipped.
     */
    @Scheduled(
            cron = "${errored.documents.daily.mail.cron}",
            zone = "UTC"
    )
    public void triggerDailyDocumentMail() {
        if (amIAllowedToRun()) {
            LOG.info("Daily Errored Document Scheduler Triggered on host: {}", HOSTNAME);
            homeSearchService.sendDailyDocumentListMail();
        } else {
            LOG.debug("Scheduler skipped on host: {}", HOSTNAME);
        }
    }

    /**
     * US-18451 
     * Checks if the scheduler should run on this host.
     * Reads allowed hosts from cache or configuration.
     * Returns true if current host is in the allowed hosts list.
     *
     * @return boolean indicating if scheduler can run on this host
     */
    private boolean amIAllowedToRun() {
        String allowedHosts = cacheConfig.getCacheValue(ALLOWED_HOSTS_CACHE_KEY);
        if (StringUtils.isBlank(allowedHosts)) {
            LOG.warn("Allowed hosts for Daily Errored Document Scheduler not configured or empty.");
            return false;
        }

        String[] hostsArray = allowedHosts.split(",");
        for (String allowedHost : hostsArray) {
            if (HOSTNAME.equalsIgnoreCase(allowedHost.trim())) {
                return true;
            }
        }
        return false;
    }

}