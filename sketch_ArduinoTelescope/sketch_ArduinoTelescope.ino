#include <AccelStepper.h>
#include <MultiStepper.h>
//#include <NeoSWSerial.h>

double SECONDS_PER_STEP = 0.005921;
double MAX_SPEED = 500;
double MAX_ALT = 90;
double MIN_ALT = 0;
String val;
//NeoSWSerial bluetooth(10, 11);

boolean altRead;
boolean azRead;
boolean x;
boolean y;
boolean en;
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
  stepperAz.setEnablePin(6);
  stepperAlt.setMaxSpeed(MAX_SPEED);
  stepperAlt.setEnablePin(7);
  altOld = 0;
  azOld = 0;
}

double getPositionInSteps(double newVal) {
  return  newVal / SECONDS_PER_STEP;
}

double getDeltaInSteps(double oldVal, double newVal) {
  return abs((oldVal - newVal) / SECONDS_PER_STEP);
}

void moveSteppers(double azNew, double altNew) {
  double azPosition = getPositionInSteps(azNew);
  double azDelta = getDeltaInSteps(azOld, azNew);
  Serial.println("azPosition=" + String(azPosition, 5));
  Serial.println("azDelta=" + String(azDelta, 5));
  if (azDelta > 0) stepperAlt.setSpeed(azDelta);
  positions[0] = round(azPosition);

  double altPosition = getPositionInSteps(altNew);
  double altDelta = getDeltaInSteps(altOld, altNew);
  Serial.println("altPosition=" + String(altPosition, 5));
  Serial.println("altDelta=" + String(altDelta, 5));

  if (altDelta > 0) stepperAlt.setSpeed(altDelta);
  positions[1] = round(altPosition);

  MultiStepper steppers;
  steppers.addStepper(stepperAz);
  steppers.addStepper(stepperAlt);

  steppers.moveTo(positions);
  unsigned long mls = millis();
  steppers.runSpeedToPosition();

  Serial.println("Current position: " + String(stepperAz.currentPosition()) + ", " + String(stepperAlt.currentPosition()));
  Serial.println("Mls "+String(millis() - mls));
}

void updateCurrentPosition(double azNew, double altNew) {
  double azPosition = getPositionInSteps(azNew);
  double altPosition = getPositionInSteps(altNew);

  Serial.println("updating position to " + String(azPosition) + ", " + String(altPosition));

  stepperAz.setCurrentPosition(round(azPosition));
  stepperAlt.setCurrentPosition(round(altPosition));

  Serial.println("Current position: " + String(stepperAz.currentPosition()) + ", " + String(stepperAlt.currentPosition()));
}
/*
  void moveSteeperOnSpeed(AccelStepper stepper, int force) {
  Serial.println("pos=" + String(force) + ", speed=" + String(abs(force)));
  stepper.move(round(force));
  stepper.setSpeed(abs(force));
  unsigned long mls = millis();
  stepper.runSpeedToPosition();
  Serial.println(String(millis() - mls));
  }
*/
boolean checkAlt(double altNew) {
  if (altNew > MAX_ALT) return false;
  if (altNew < MIN_ALT) return false;
  return true;
}

void loop() {
  if (Serial.available()) {
    char c = Serial.read();
    if (c == '#') {
      Serial.println("#");
      Serial.println("val=" + val);
      if (azRead) {
        azNew = val.toDouble();
        azRead = false;
      } else if (altRead) {
        altNew = val.toDouble();
        Serial.println("altNew=" + String(altNew));
        Serial.println("altOld=" + String(altOld));
        Serial.println("azNew=" + String(azNew));
        Serial.println("azOld=" + String(azOld));
        if (altOld == 0 && azOld == 0) {
          updateCurrentPosition(azNew, altNew);
        } else {
          moveSteppers(azNew, altNew);
        }
        altOld = altNew;
        azOld = azNew;
        altRead = false;
        azRead = false;
      } else if (x) {
        azNew = azOld + (val.toDouble() * SECONDS_PER_STEP);
        x = false;
      } else if (y) {
        altNew = altOld + (val.toDouble() * SECONDS_PER_STEP);
        if (checkAlt(altNew)) {
          moveSteppers(azNew, altNew);
          altOld = altNew;
          azOld = azNew;
        }
        y = false;
      } else if (en) {
        if (val == "1") {
          stepperAz.disableOutputs();
          stepperAlt.disableOutputs();
        } else {
          stepperAz.enableOutputs();
          stepperAlt.enableOutputs();
        }
        en = false;
      }
      val = "";
    } else if (c == 'A') {
      Serial.println("A");
      altRead = true;
      azRead = false;
      x = false;
      y = false;
      en = false;
      val = "";
    } else if (c == 'Z') {
      Serial.println("Z");
      altRead = false;
      azRead = true;
      x = false;
      y = false;
      en = false;
      val = "";
    } else if (c == 'X') {
      Serial.println("X");
      x = true;
      y = false;
      altRead = false;
      azRead = false;
      en = false;
      val = "";
    } else if (c == 'Y') {
      Serial.println("Y");
      x = false;
      y = true;
      altRead = false;
      azRead = false;
      en = false;
      val = "";
    } else if (c == 'S') {
      azOld = 0;
      altOld = 0;
      x = false;
      y = false;
      altRead = false;
      azRead = false;
      en = false;
      val = "";
    } else if (c == 'E') {
      x = false;
      y = false;
      altRead = false;
      azRead = false;
      en = true;
      val = "";
    } else {
      val += c;
    }
  }
}