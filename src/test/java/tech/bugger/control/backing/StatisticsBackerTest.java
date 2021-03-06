package tech.bugger.control.backing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.LogExtension;
import tech.bugger.business.exception.DataAccessException;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.StatisticsService;
import tech.bugger.business.service.TopicService;
import tech.bugger.business.util.Feedback;
import tech.bugger.business.util.Registry;
import tech.bugger.global.transfer.ReportCriteria;
import tech.bugger.global.transfer.Topic;

import javax.enterprise.event.Event;
import javax.faces.context.ExternalContext;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(LogExtension.class)
@ExtendWith(MockitoExtension.class)
public class StatisticsBackerTest {

    @Mock
    @SuppressWarnings("unused")
    private StatisticsService statisticsService;

    @Mock
    private TopicService topicService;

    @Mock
    private UserSession userSession;

    @Mock
    private Event<Feedback> feedbackEvent;

    @Mock
    private Registry registry;

    @Mock
    private ExternalContext ectx;

    @Mock
    private ResourceBundle resourceBundle;

    @InjectMocks
    private StatisticsBacker statisticsBacker;

    @BeforeEach
    public void setUp() {
        lenient().doReturn(Locale.ENGLISH).when(userSession).getLocale();
        lenient().doReturn(resourceBundle).when(registry).getBundle(anyString(), any());
    }

    @Test
    public void testInitWhenNoParam() {
        doReturn(Collections.emptyMap()).when(ectx).getRequestParameterMap();
        statisticsBacker.init();
        assertEquals("", statisticsBacker.getReportCriteria().getTopic());
    }

    @Test
    public void testInitWhenParamNotInteger() {
        doReturn(Map.of("t", "noint")).when(ectx).getRequestParameterMap();
        statisticsBacker.init();
        assertEquals("", statisticsBacker.getReportCriteria().getTopic());
    }

    @Test
    public void testInitWhenParamNotIDOfExistingTopic() {
        doReturn(Map.of("t", "-1")).when(ectx).getRequestParameterMap();
        doReturn(null).when(topicService).getTopicByID(anyInt());
        statisticsBacker.init();
        assertEquals("", statisticsBacker.getReportCriteria().getTopic());
    }

    @Test
    public void testInitWhenParamIDOfExistingTopic() {
        doReturn(Map.of("t", "1")).when(ectx).getRequestParameterMap();
        Topic topic = mock(Topic.class);
        doReturn("title").when(topic).getTitle();
        doReturn(topic).when(topicService).getTopicByID(anyInt());
        statisticsBacker.init();
        assertEquals("title", statisticsBacker.getReportCriteria().getTopic());
    }

    @Test
    public void testInitWhenDataLoadError() throws Exception {
        doThrow(DataAccessException.class).when(statisticsService).averageTimeOpen(any());
        statisticsBacker.init();
        verify(feedbackEvent).fire(new Feedback(any(), Feedback.Type.ERROR));
    }

    @Test
    public void testApplyFiltersWhenSuccess() {
        assertNull(statisticsBacker.applyFilters());
        verify(feedbackEvent).fire(new Feedback(any(), Feedback.Type.INFO));
    }

    @Test
    public void testApplyFiltersWhenError() throws Exception {
        doThrow(DataAccessException.class).when(statisticsService).averageTimeOpen(any());
        statisticsBacker.applyFilters();
        verify(feedbackEvent).fire(new Feedback(any(), Feedback.Type.ERROR));
    }

    @Test
    public void testGetOpenReportCount() {
        assertDoesNotThrow(() -> statisticsBacker.getOpenReportCount());
    }

    @Test
    public void testGetAverageTimeOpen() {
        assertDoesNotThrow(() -> statisticsBacker.getAverageTimeOpen());
    }

    @Test
    public void testGetAveragePostsPerReport() {
        assertDoesNotThrow(() -> statisticsBacker.getAveragePostsPerReport());
    }


    @Test
    public void testGetTopReports() {
        assertDoesNotThrow(() -> statisticsBacker.getTopReports());
    }

    @Test
    public void testGetTopUsers() {
        assertDoesNotThrow(() -> statisticsBacker.getTopUsers());
    }

    @Test
    public void testGetTopicTitles() {
        assertDoesNotThrow(() -> statisticsBacker.getTopicTitles());
    }


    @Test
    public void testSetReportCriteria() {
        ReportCriteria criteria = mock(ReportCriteria.class);
        statisticsBacker.setReportCriteria(criteria);
        assertEquals(criteria, statisticsBacker.getReportCriteria());
    }

}