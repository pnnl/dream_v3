import h5py
import re
import glob
import sys
import os
import pickle
import numpy as np
import matplotlib as mpl
mpl.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
from mpl_toolkits import mplot3d
from mpl_toolkits.mplot3d import Axes3D


csv_dir   = sys.argv[1]
csv_fname0 = sys.argv[2]

input_dir = sys.argv[3]
input_prefix = 'sim_'

output_dir = sys.argv[4]


files = os.listdir(input_dir)

file = h5py.File(input_dir+'/'+files[0],'r')
xmin = np.min(file['data']['x'])
xmax = np.max(file['data']['x'])
ymin = np.min(file['data']['y'])
ymax = np.max(file['data']['y'])
zmin = np.min(file['data']['z'])
zmax = np.max(file['data']['z'])
times = np.array(file['data']['times'])

nx,ny,nz=np.array(file['plot0']['pressure']).shape
nt = len(times)

vx = np.array(file['data']['vertex-x'])
vy = np.array(file['data']['vertex-y'])
vz = np.array(file['data']['vertex-z'])
x = np.array(file['data']['x'])
y = np.array(file['data']['y'])
z = np.array(file['data']['z'])

pres=np.zeros([nLeaks,nx,ny,nz,nt])
satu=np.zeros([nLeaks,nx,ny,nz,nt])
grav=np.zeros([nLeaks,nx,ny,nt])
for n in range(nLeaks):
  file = h5py.File(input_dir+'/'+files[n],'r')
  for it in range(nt):
    //pres[n,:,:,:,it] = np.array(file['plot%i'%it]['pressure'])
    satu[n,:,:,:,it] = np.array(file['plot%i'%it]['saturation'])*100

mass = 0
for n in range(nLeaks):
  mass += np.sum(satu[n,:,:,:,-1])

mass *= (vx[1]-vx[0])*(vy[1]-vy[0])*(vz[1]-vz[0])       # volume
mass *= porosity	# porosity
mass *= 1000.0		# m3 to kg
mass /= 1000.0		# kg to tonne
mass /= 1.0e6		# tonne to MT



# reading in the csv file
file = open(csv_dir+'/'+csv_fname0,'rb')
line = file.readline()
perc0   = []
ttd0    = []
vad0    = []
cost0   = []
nWells0 = []
xyz0    = {}
det0    = {}
i = 0
for line in file:
  if 'No detection' in str(line):
    continue

  if str(line).split(',')[1].split('%')[0]=='None': perc0 += [0]
  else: perc0   += [float(str(line).split(',')[1].split('%')[0])]
  ttd0    += [float(str(line).split(',')[3].split(' ')[0])]
  vad0    += [float(str(line).split(',')[4].split(' ')[0])]
  cost0   += [float(str(line).split(',')[5].split('$')[1])]
  nWells0 += [int(str(line).split(',')[6])]
  xyz0[i]=[]
  for isens in range(len( str(line).split(',')[7:] )):
    xyz0[i] += [str(line).split(',')[7:][isens]]
  det0[i] = []
  for leak in str(line).split(',')[1].split(' ')[1:]:
    det0[i] += [int(leak.split('_')[1])-1]
  i+=1
perc0 = np.array(perc0,dtype='float').reshape([len(perc0),1])
ttd0  = np.array(ttd0,dtype='float').reshape([len(ttd0),1])
vad0  = np.array(vad0,dtype='float').reshape([len(vad0),1])/1.0e9
cost0 = np.array(cost0,dtype='float').reshape([len(cost0),1])/1.0e3

mass0 = np.zeros([len(perc0)],dtype='float')
for i in range(len(perc0)):
  if len(det0[i])>0:
    mass0[i] = np.sum(satu[det0[i],:,:,:,-1])
mass0 *= (vx[1]-vx[0])*(vy[1]-vy[0])*(vz[1]-vz[0])       # volume
mass0 *= porosity	# porosity
mass0 *= 1000.0		# m3 to kg
mass0 /= 1000.0		# kg to tonne
mass0 /= 1.0e6		# tonne to MT
mass0 /= mass
mass0 *= 100


percn = perc0
ttdn  = ttd0
vadn  = vad0
costn = cost0
massn = mass0

percmin = np.min(percn)
percmax = np.max(percn)

ttdmin = np.min(ttdn)
ttdmax = np.max(ttdn)

vadmin = np.min(vadn)
vadmax = np.max(vadn)

massmin = np.min(massn)
massmax = np.max(massn)

costmin = np.min(costn)
costmax = np.max(costn)

dperc = (percmax-percmin)*0.05
dttd = (ttdmax-ttdmin)*0.05
dvad = (vadmax-vadmin)*0.05
dmass = (massmax-massmin)*0.05
dcost = (costmax-costmin)*0.05

percmin -= dperc
percmax += dperc
ttdmin -= dttd
ttdmax += dttd
vadmin -= dvad
vadmax += dvad
massmin -= dmass
massmax += dmass
costmin -= dcost
costmax += dcost

sizes  = [80,50,35,20,5]
colors = ['r','g','b','c','m']

norm = mpl.colors.Normalize(vmin=np.min(costn),vmax=np.max(costn))
sm   = mpl.cm.ScalarMappable(norm=norm, cmap=mpl.cm.jet)

pt=10

pt0,c0 = pt,'red'
pt1,c1 = pt,'blue'
pt2,c2 = pt,'green'

fig = plt.figure(figsize=(12,6))
spec = gridspec.GridSpec(ncols=2, nrows=1, figure=fig)
ax02 = fig.add_subplot(spec[0,0])
ax05 = fig.add_subplot(spec[0,1])
ax02.scatter(perc0,ttd0,s=pt0,c=c0,zorder=2)
ax05.scatter(mass0,ttd0,s=pt0,c=c0,zorder=2)
ax02.set_xlabel('Percent of leaks detected [%]',fontsize=14)
ax05.set_xlabel('Mass of CO2 detected [%]',fontsize=14)
ax02.set_ylabel('Time to Detection [years]',fontsize=14)
ax05.set_ylabel('Time to Detection [years]',fontsize=14)
ax02.set_xlim([percmin,percmax])
ax05.set_xlim([massmin,massmax])
ax02.set_ylim([ttdmin,ttdmax])
ax05.set_ylim([ttdmin,ttdmax])
ax02.legend(loc='upper right')
plt.tight_layout()
plt.savefig('%s/results.eps'%(output_dir,output_fname_prefix),format='eps',bbox_inches='tight')
plt.savefig('%s/results.png'%(output_dir,output_fname_prefix),format='png',bbox_inches='tight',dpi=300)
plt.close()