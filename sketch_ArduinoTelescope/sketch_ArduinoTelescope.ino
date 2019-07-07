#include <AccelStepper.h>
#include <MultiStepper.h>
//#include <NeoSWSerial.h>

double SECONDS_PER_STEP = 0.009375;
double MAX_SPEED = 500;
double MAX_ALT = 90;
double MIN_ALT = 0;
String val;
//NeoSWSerial bluetooth(10, 11);

boolean altRead;
boolean azRead;
boolean x;
boolean y;
double altNew;
double azNew;
double altOld;
double azOld;
long positions[2];
#define dirPinAlt 2
#define stepPinAlt 3
#define dirPinAz 4
#define stepPinAz 5
#define motorInterfaceType 1

AccelStepper stepperAlt = AccelStepper(motorInterfaceType, stepPinAlt, dirPinAlt);
AccelStepper stepperAz = AccelStepper(motorInterfaceType, stepPinAz, dirPinAz);
void setup() {
  Serial.begin(9600);
  //bluetooth.begin(9600);
  stepperAz.setMaxSpeed(MAX_SPEED);
  stepperAlt.setMaxSpeed(MAX_SPEED);
}

double getDeltaInSteps(double oldVal, double newVal) {
  return ((oldVal - newVal) / SECONDS_PER_STEP);
}

void moveSteppers(double azNew, double altNew) {
  double azDelta = getDeltaInSteps(azOld, azNew);
  Serial.println("azDelta=" + String(azDelta, 5));
  if (azDelta >= 1 || azDelta <= -1) {
    stepperAlt.setSpeed(abs(azDelta));
    positions[0] = round(azDelta);
  }
  double altDelta = getDeltaInSteps(altOld, altNew);
  Serial.println("altDelta=" + String(altDelta, 5));
  if (altDelta >= 1 || altDelta <= -1) {
    stepperAlt.setSpeed(abs(altDelta));
    positions[1] = round(altDelta);
  }

  MultiStepper steppers;
  steppers.addStepper(stepperAz);
  steppers.addStepper(stepperAlt);

  steppers.moveTo(positions);
  unsigned long mls = millis();
  steppers.runSpeedToPosition();
  Serial.println(String(millis() - mls));
}

void moveSteeperOnSpeed(AccelStepper stepper, int force) {
  Serial.println("pos=" + String(force) + ", speed=" + String(abs(force)));
  stepper.moveTo(round(force));
  stepper.setSpeed(abs(force));
  unsigned long mls = millis();
  stepper.run();
  Serial.println(String(millis() - mls));
}

boolean checkAlt(double value) {
  if (!altOld) return true;
  if (altOld + value > MAX_ALT) return false;
  if (altOld + value < MIN_ALT) return false;
  return true;
}

void loop() {
  if (Serial.available()) {
    char c = Serial.read();
    if (c == '#') {
      if (azRead) {
        azNew = val.toDouble();
        azRead = false;
      } else if (altRead) {
        altNew = val.toDouble();
        if (altOld != 0 && azOld != 0  &&
            (altNew != altOld || azNew != azOld)) {
          moveSteppers(azNew, altNew);
        }
        altOld = altNew;
        azOld = azNew;
        altRead = false;
        azRead = false;
      } else if (x) {
        int xVal = val.toInt();
        moveSteeperOnSpeed(stepperAz, xVal);
        azOld = azOld + (xVal * SECONDS_PER_STEP);
        x = false;
      } else if (y) {
        int yVal = val.toInt();
        if (checkAlt(yVal)) {
          moveSteeperOnSpeed(stepperAlt, yVal);
          altOld = altOld + (yVal * SECONDS_PER_STEP);
          y = false;
        }
      }
      val = "";
    } else if (c == 'A') {
      altRead = true;
      azRead = false;
      x = false;
      y = false;
      val = "";
    } else if (c == 'Z') {
      altRead = false;
      azRead = true;
      x = false;
      y = false;
      val = "";
    } else if (c == 'X') {
      x = true;
      y = false;
      altRead = false;
      azRead = false;
      val = "";
    } else if (c == 'Y') {
      x = false;
      y = true;
      altRead = false;
      azRead = false;
      val = "";
    } else if (val == "STOP") {
      azOld = 0;
      altOld = 0;
      x = false;
      y = false;
      altRead = false;
      azRead = false;
      val = "";
    } else {
      val += c;
    }
  }
}