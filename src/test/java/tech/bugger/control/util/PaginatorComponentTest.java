package tech.bugger.control.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bugger.LogExtension;

import javax.faces.component.UIComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(LogExtension.class)
@ExtendWith(MockitoExtension.class)
public class PaginatorComponentTest {

    private PaginatorComponent component;

    @Mock
    private UIComponent refreshBtn;

    @BeforeEach
    public void setup() {
        component = new PaginatorComponent();
        component.setRefreshBtn(refreshBtn);
    }

    @Test
    public void testGetRefreshBtnID() {
        String id = "cb-refresh";
        doReturn(id).when(refreshBtn).getClientId();
        assertAll(() -> assertEquals(id, component.getRefreshBtnID()),
                () -> assertEquals(refreshBtn, component.getRefreshBtn()));
    }

}