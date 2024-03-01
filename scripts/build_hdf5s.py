import numpy as np
import h5py

import matplotlib as mpl
mpl.use('Agg')
import matplotlib.pyplot as plt

def generate_leaks(nx,ny,nz,nt):
  pres = np.random.uniform(0,1,[nx,ny,nz,nt])
  satu = np.random.uniform(0,1,[nx,ny,nz,nt])
  grav = np.random.uniform(0,1,[nx,ny,nz,nt])
  return pres,satu,grav

nx = 25
ny = 25
nz = 3
nt = 72

xmin,xmax = 0,2500
ymin,ymax = 0,2500
zmin,zmax = 0,200

nRealizations = 10
for iRealization in range(nRealizations):
  pres,satu,grav = generate_leaks(nx,ny,nz,nt)

  hdf5 = h5py.File('synth_%05i.h5'%iRealization,'w')
  for i in range(nt):
    g1=hdf5.create_group('plot%i'%i)
    g1.create_dataset('pressure',data=pres[:,:,:,i],dtype='float32')
    g1.create_dataset('saturation',data=satu[:,:,:,i],dtype='float32')
    g1.create_dataset('gravity',data=grav[:,:,:,i],dtype='float32')

    g1['pressure'].attrs['unit'] = 'kPa'
    g1['saturation'].attrs['unit'] = '1'
    g1['gravity'].attrs['unit'] = 'mGal'

  vx = np.linspace(xmin, xmax, nx+1)
  vy = np.linspace(ymin, ymax, ny+1)
  vz = np.linspace(zmin, zmax, nz+1)

  x = np.linspace( (vx[0]+vx[1])/2.0, (vx[-2]+vx[-1])/2.0, nx)
  y = np.linspace( (vy[0]+vy[1])/2.0, (vy[-2]+vy[-1])/2.0, ny)
  z = np.linspace( (vz[0]+vz[1])/2.0, (vz[-2]+vz[-1])/2.0, nz)

  porosity_fixed = 0.4

  g1=hdf5.create_group('data')
  g1.create_dataset('porosity', data=porosity_fixed*np.ones([nx,ny,nz]),dtype='float32')
  g1.create_dataset('steps',    data=np.array(range(nt)),dtype='float32')
  g1.create_dataset('times',    data=np.array(range(nt)),dtype='float32')
  g1.create_dataset('vertex-x', data=np.array(vx),dtype='float32')
  g1.create_dataset('vertex-y', data=np.array(vy),dtype='float32')
  g1.create_dataset('vertex-z', data=np.array(vz),dtype='float32')
  g1.create_dataset('x',        data=np.array(x),dtype='float32')
  g1.create_dataset('y',        data=np.array(y),dtype='float32')
  g1.create_dataset('z',        data=np.array(z),dtype='float32')

  g1['x'].attrs['units'] = 'm'
  g1['y'].attrs['units'] = 'm'
  g1['z'].attrs['units'] = 'm'
  g1['vertex-x'].attrs['units'] = 'm'
  g1['vertex-y'].attrs['units'] = 'm'
  g1['vertex-z'].attrs['units'] = 'm'

  g1['z'].attrs['postive'] = 'up'
  g1['vertex-z'].attrs['postive'] = 'up'

  g1=hdf5.create_group('statistics')
  g1.create_dataset('pressure',   data=np.array([ np.min(pres),np.mean(pres),np.max(pres) ]),dtype='float32')
  g1.create_dataset('saturation', data=np.array([ np.min(satu),np.mean(satu),np.max(satu) ]),dtype='float32')
  g1.create_dataset('gravity',    data=np.array([ np.min(grav),np.mean(grav),np.max(grav) ]),dtype='float32')

  hdf5.close()

  plt.figure(figsize=(20,6))

  plt.subplot(131)
  plt.imshow(pres[:,:,1,-1],origin='lower',extent=(np.min(x),np.max(x),np.min(y),np.max(y)),cmap=mpl.cm.jet,vmin=np.min(pres),vmax=np.max(pres))
  plt.xlabel('Easting [m]',fontsize=14)
  plt.ylabel('Northing [m]',fontsize=14)
  plt.colorbar()
  plt.title('Pressure')

  plt.subplot(132)
  plt.imshow(satu[:,:,1,-1],origin='lower',extent=(np.min(x),np.max(x),np.min(y),np.max(y)),cmap=mpl.cm.jet,vmin=np.min(satu),vmax=np.max(satu))
  plt.xlabel('Easting [m]',fontsize=14)
  plt.ylabel('Northing [m]',fontsize=14)
  plt.colorbar()
  plt.title('Saturation')

  plt.subplot(133)
  plt.imshow(grav[:,:,1,-1],origin='lower',extent=(np.min(x),np.max(x),np.min(y),np.max(y)),cmap=mpl.cm.jet,vmin=np.min(grav),vmax=np.max(grav))
  plt.xlabel('Easting [m]',fontsize=14)
  plt.ylabel('Northing [m]',fontsize=14)
  plt.colorbar()
  plt.title('Gravity')

  plt.savefig('./figures/slide_%03i.png'%iRealization,format='png',bbox_inches='tight')
  plt.close()
