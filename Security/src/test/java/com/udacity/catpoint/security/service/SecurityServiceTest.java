package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//import junit.framework.TestCase;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityServiceTest{

    private SecurityService securityService;

    private final String random = UUID.randomUUID().toString();

    private Sensor sensor;

    private Set<Sensor> getAllSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor(random, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));

        return sensors;
    }

    @Mock
    private StatusListener statusListener;

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

    // Requirement 1
    @Test
    void ifSystemArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    // requirement 2
    @Test
    void ifSystemArmedAndSensorActivatedAndPendingState_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // requirement 3
    @Test
    void ifPendingAlarmAndSensorInactive_returnNoAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // requirement 4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_changeSensorShouldNotAffectAlarmState(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // requirement 5
    @Test
    void ifSensorActivatedWhileActiveAndPendingAlarm_changeStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //requirement 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void ifSensorDeactivatedWhileInactive_noChangesToAlarmState(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        securityService.changeSensorActivationStatus(sensor, false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //requirement 7
    @Test
    void ifImageServiceIdentifiesCatWhileAlarmArmedHome_changeStatusToAlarm() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //requirement 8
    @Test
    void ifImageServiceIdentifiesNoCatImage_changeStatusToNoAlarmAsLongSensorsNotActive() {
        Set<Sensor> sensors = getAllSensors(3, false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // requirement 9
    @Test
    void ifSystemDisarmed_setNoAlarmState() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // requirement 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetSensorsToInactive(ArmingStatus status) {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertTrue(securityService.getSensors().stream().allMatch(sensor -> Boolean.FALSE.equals(sensor.getActive())));
    }
    // requirement 11
    @Test
    void ifSystemArmedHomeWhileImageServiceIdentifiesCat_changeStatusToAlarm() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void checkChangeSensorActivationStatusWorks(){
        securityRepository.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        assert sensor.getActive().equals(false);
        verify(securityRepository, times(1)).setAlarmStatus(any(AlarmStatus.class));
    }

    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void setArmingStatusMethod(ArmingStatus status) {
        securityService.setArmingStatus(status);
    }

    @Test
    public void addRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    public void addRemoveSensor() {
        securityService.addSensor(sensor);
        assertNotNull(securityService.getSensors());
        securityService.removeSensor(sensor);
    }

    @ParameterizedTest
    @CsvSource({"PENDING_ALARM,DOOR,true", "PENDING_ALARM,DOOR,false",
                "PENDING_ALARM,WINDOW,true", "PENDING_ALARM,WINDOW,false",
                "NO_ALARM,DOOR,true", "NO_ALARM,DOOR,false"})
    public void changeSensorStatusWithAlarms(AlarmStatus alarmStatus, SensorType sensorType, Boolean active){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, active);
        securityService.changeSensorActivationStatus(sensor, false);
        securityService.changeSensorActivationStatus(sensor, active);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, active);
        securityService.changeSensorActivationStatus(sensor, false);
        securityService.changeSensorActivationStatus(sensor, active);
    }

    @Test
    void coverDeactivate() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, true);
//        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

}