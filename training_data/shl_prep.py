import numpy as np
import csv

NUM_DIRS = 17
dirlist = []
for i in range(1, NUM_DIRS + 1):
    dirlist.append(str(i))

# Clear files and write header
dataExplanation = [
    "accX", "accY", "accZ",
    "gyroX", "gyroY", "gyroZ",
    "magX", "magY", "magZ",
    "orientW", "orientX", "orientY", "orientZ",
    "gravX", "gravY", "gravZ",
    "linAccX", "linAccY", "linAccZ",
    "Pressure",
    "Altitude",
    "Temperature"
]
labelsExplanation = "Label (Null=0, Still=1, Walking=2, Run=3, Bike=4, Car=5, Bus=6, Train=7, Subway=8)"
samplesPerWindow = 50 # ~10 sec time window at 5 Hz
fDataFileStr = "processed/hips_motion_cleaned.csv"
fLabelFileStr = "processed/label_cleaned.csv"

fData = open(fDataFileStr,'r+')
fLabels = open(fLabelFileStr,'r+')

dataWriter = csv.writer(fData)
dataWriter.writerow(dataExplanation)

labelsWriter = csv.writer(fLabels)
labelsWriter.writerow(labelsExplanation)

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
    
    # Purge Null class values
    j = 0
    rowsToDelete = []
    for j in range(0, data.shape[0]):
        if(labels[j][1] == 0): 
            rowsToDelete.append(j)    
    data = np.delete(data, rowsToDelete, 0)
    labels = np.delete(labels, rowsToDelete, 0)
    print(data.shape) 
    
    # Average remaining samples to make time window time = window_time
    outData = [[]*(data.shape[1] - 1)] # Exclude time
    outLabels = []
    sumValues = [data.shape[1]]
    sampleCnt = 0
    sumValues = data[0][1:data.shape[1]]
    for j in range(1, data.shape[0]):
        if(labels[j][1] == labels[j - 1][1]):
            if(sampleCnt >= samplesPerWindow):
                outData.append(sumValues/samplesPerWindow)
                outLabels.append(labels[j][1])
                sampleCnt = 0
                sumValues = sumValues*0
            sumValues = sumValues + data[j][1:data.shape[1]]
            sampleCnt += 1
        else:
            outData.append(sumValues/sampleCnt)
            outLabels.append(labels[j - 1][1])
            sampleCnt = 0
            sumValues = data[j][1:data.shape[1]]
    outData.pop(0)
    outData = np.array(outData, dtype = float)
    outLabels = np.array(outLabels, dtype = int)
    print(outData.shape)
    np.savetxt(fData, outData, fmt='%.10f', delimiter=",")
    np.savetxt(fLabels, outLabels, fmt='%d', delimiter=",")
    print(f"Processed data from folder {i+1} appended to file.")
    
