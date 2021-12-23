package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import junit.framework.TestCase;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest extends TestCase{

    private SecurityService securityService;

    private final String random = UUID.randomUUID().toString();

    private Sensor sensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getSensor();
    }

    private Sensor getSensor() {
        return new Sensor(random, SensorType.DOOR);
    }

    @Test
    void ifSystemArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
}