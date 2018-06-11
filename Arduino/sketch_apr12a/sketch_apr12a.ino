#include <SoftwareSerial.h> 
 
SoftwareSerial ModBluetooth(2, 3); // RX | TX 
int pin1 = 10, pin2 = 9;
 
void setup()  
{ 
    pinMode(4, OUTPUT); 
    pinMode(pin1, OUTPUT); 
    pinMode(pin2, OUTPUT); 
    digitalWrite(4, LOW);  
     
    ModBluetooth.begin(9600); 
    Serial.begin(9600);  
    ModBluetooth.println("MODULO CONECTADO");  
    ModBluetooth.print("#");  
} 
 
void loop()  
{  
    if (ModBluetooth.available())  
    { 
        char VarChar; 
         
        VarChar = ModBluetooth.read(); 
         
        if(VarChar == '1') 
        { 
        digitalWrite(4, HIGH); 
        delay(100); 
        ModBluetooth.print("LED ENCENDIDO"); 
        Serial.print("LED ENCENDIDO"); 
        ModBluetooth.print("#"); 
        } 
        else if(VarChar == '0') 
        { 
        digitalWrite(4, LOW); 
        delay(100); 
        ModBluetooth.print("LED APAGADO#"); 
        Serial.print("LED APAGADO#"); 
        }
        else if(VarChar == '3')
        {
          digitalWrite(pin1, 0);
          delay(100);
          digitalWrite(pin2, 1);
          delay(300);
          digitalWrite(pin2, 0);
        }
        else if(VarChar == '4')
        {
          digitalWrite(pin1, 1);
          delay(100);
          digitalWrite(pin2, 0);
          delay(200);
          digitalWrite(pin1, 0);
        }
    } 
} 
