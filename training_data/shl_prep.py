import numpy as np
import csv
import os

# Clear files and write header
dataExplanation = [
    "accX", "accY", "accZ", 
    "gyroX", "gyroY", "gyroZ",
    "magX", "magY", "magZ", 
    "orientW", "orientX", "orientY", "orientZ",
    "linAccX", "linAccY", "linAccZ", 
    "pressure", 
    "accMag", "gyroMag", "magMag", "linAccMag"
]
labelsExplanation = ["label"]
samplesPerWindow = 50 # ~10 sec time window at 5 Hz
fDataFileStr = "processed/hips_motion_cleaned.csv"
fLabelFileStr = "processed/label_cleaned.csv"

fData = open(fDataFileStr,'r+')
fLabels = open(fLabelFileStr,'r+')

dataWriter = csv.writer(fData)
dataWriter.writerow(dataExplanation)

labelsWriter = csv.writer(fLabels)
labelsWriter.writerow(labelsExplanation)

path = "original/"
dirlist = os.listdir(path)
print(dirlist)

for i in range(0, len(dirlist)):
    oDataFileStr = "original/" + dirlist[i] + "/Hips_Motion.txt"
    oLabelFileStr = "original/" + dirlist[i] + "/Label.txt"
    
    data = np.loadtxt(oDataFileStr)
    labels = np.loadtxt(oLabelFileStr)
    print(data.shape)
    
    # Downsample to 5Hz (original sample rate 100Hz)
    data = data[0:data.size:20]
    labels = labels[0:labels.size:20]
    print(data.shape)
    
    # Purge coloumn 0 (time), 14-16 (gravity), 21 (altitude) and 22 (temperature)
    data = np.delete(data, [0, 14, 15, 16, 21, 22], axis=1)
    
    # Purge coloumn 0 (time), 2-7 (other labels)
    labels = np.delete(labels, [0, 2, 3, 4, 5, 6, 7], axis=1)
    labels = labels.reshape((labels.shape[0],))
    
    # Purge Null class values and NaN values
    rowsToDelete = []
    for j in range(0, data.shape[0]):
        if(labels[j] == 0 or np.any(np.isnan(data[j]))): 
            rowsToDelete.append(j)    
    data = np.delete(data, rowsToDelete, 0)
    labels = np.delete(labels, rowsToDelete, 0)
    print(data.shape) 
    
    # Average remaining samples to make time window time = window_time
    if(data.shape[0] > 0):
        outData = [[]*(data.shape[1] + 4)] # add room for magnitudes
        outLabels = []
        sumValues = [data.shape[1] + 4]
        tempData = [data.shape[1] + 4]
        sampleCnt = 0
        
        # Add magnitudes to tempData
        tempData = np.append(tempData, np.sqrt(data[0][0]**2 + data[0][1]**2 + data[0][2]**2))
        tempData = np.append(tempData, np.sqrt(data[0][3]**2 + data[0][4]**2 + data[0][5]**2))
        tempData = np.append(tempData, np.sqrt(data[0][6]**2 + data[0][7]**2 + data[0][8]**2))
        tempData = np.append(tempData, np.sqrt(data[0][13]**2 + data[0][14]**2 + data[0][15]**2))
        
        for j in range(1, data.shape[0]):
            if(labels[j] == labels[j - 1]):
                if(sampleCnt >= samplesPerWindow):
                    if(not np.any(np.isnan(sumValues/samplesPerWindow))):
                        outData.append(sumValues/samplesPerWindow)
                        outLabels.append(labels[j])
                    sampleCnt = 0
                    sumValues = sumValues*0
                    tempData = tempData*0
            
                # Calculate magnitude of acceleration, gyroscope, magnetometer and Linear Acceleration
                tempData = data[j]
                tempData = np.append(tempData, np.sqrt(data[j][0]**2 + data[j][1]**2 + data[j][2]**2))
                tempData = np.append(tempData, np.sqrt(data[j][3]**2 + data[j][4]**2 + data[j][5]**2))
                tempData = np.append(tempData, np.sqrt(data[j][6]**2 + data[j][7]**2 + data[j][8]**2))
                tempData = np.append(tempData, np.sqrt(data[j][13]**2 + data[j][14]**2 + data[j][15]**2))
                
                sumValues = sumValues + tempData
                sampleCnt += 1
            else:
                if(not np.any(np.isnan(sumValues/sampleCnt))):
                    outData.append(sumValues/sampleCnt)
                    outLabels.append(labels[j - 1])
                sampleCnt = 0
                tempData = data[j]
                tempData = np.append(tempData, np.sqrt(data[j][0]**2 + data[j][1]**2 + data[j][2]**2))
                tempData = np.append(tempData, np.sqrt(data[j][3]**2 + data[j][4]**2 + data[j][5]**2))
                tempData = np.append(tempData, np.sqrt(data[j][6]**2 + data[j][7]**2 + data[j][8]**2))
                tempData = np.append(tempData, np.sqrt(data[j][13]**2 + data[j][14]**2 + data[j][15]**2))
                sumValues = tempData
        outData.pop(0)
        outData = np.array(outData, dtype = float)
        print(outData.shape)
        outLabels = np.array(outLabels, dtype = int)
        np.savetxt(fData, outData, fmt='%.10f', delimiter=",")
        np.savetxt(fLabels, outLabels, fmt='%d', delimiter=",")
        print(f"Processed data from folder {dirlist[i]} appended to file.")
    else:
       print("File is only NaN or null-class") 
    
