################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../src/DuinoBotKids.v1.xx_0022/LedMatrix8x8/LEDMatrix8x8.cpp 

OBJS += \
./src/DuinoBotKids.v1.xx_0022/LedMatrix8x8/LEDMatrix8x8.o 

CPP_DEPS += \
./src/DuinoBotKids.v1.xx_0022/LedMatrix8x8/LEDMatrix8x8.d 


# Each subdirectory must supply rules for building sources it contributes
src/DuinoBotKids.v1.xx_0022/LedMatrix8x8/%.o: ../src/DuinoBotKids.v1.xx_0022/LedMatrix8x8/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: AVR C++ Compiler'
	avr-g++ -I"/home/mariano/Desktop/facultad/robotica/repos/robots/RobotProtocol/src/DuinoBotKids.v1.xx_0022" -Wall -Os -funsigned-char -funsigned-bitfields -fno-exceptions -ffunction-sections -fdata-sections --pedantic -mmcu=atmega32u4 -DF_CPU=16000000UL -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -c -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


