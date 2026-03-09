package org.santalina.diving.unit;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santalina.diving.service.ConfigService;
import org.santalina.diving.service.SlotService;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de SlotService — ConfigService est mocké.
 */
@QuarkusTest
class SlotServiceTest {

    @Inject
    SlotService slotService;

    @InjectMock
    ConfigService configService;

    @BeforeEach
    void setup() {
        when(configService.getMaxDivers()).thenReturn(25);
        when(configService.getSlotMinHours()).thenReturn(1);
        when(configService.getSlotMaxHours()).thenReturn(10);
        when(configService.getSlotResolutionMinutes()).thenReturn(15);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenSlotDoesNotExist() {
        assertThrows(NotFoundException.class,
                () -> slotService.getById(99999L));
    }

    @Test
    void getSlotsByDate_shouldReturnEmptyList_whenNoSlotsOnDate() {
        var result = slotService.getSlotsByDate(LocalDate.of(2099, 1, 1));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSlotsByWeek_shouldReturnEmptyList_whenNoSlotsInWeek() {
        var result = slotService.getSlotsByWeek(LocalDate.of(2099, 1, 1));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSlotsByMonth_shouldReturnEmptyList_whenNoSlotsInMonth() {
        var result = slotService.getSlotsByMonth(2099, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
