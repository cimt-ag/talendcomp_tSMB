/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cimt.talendcomp.tsmb;

import java.util.Comparator;

/**
 *
 * @author dkoch
 */
public enum SMBFileStructComparators implements Comparator<SMBFileStruct>{   
    B{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return FILENAME_DIRSTART.compare(o1, o2);
        }
    },
    C{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return FILENAME_DIRAFTER.compare(o1, o2);
        }
    },
    D{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return SIZE.compare(o1, o2);
        }
    },
    E{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return DATE.compare(o1, o2);
        }
    },
    NONE{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return 1;
        }
    },
    FILENAME{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return o1.name.compareTo(o2.name);
        }
    },
    PATH{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    },
    FILENAME_DIRSTART{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            if(o1.directory == o2.directory){
                return FILENAME.compare(o1, o2);
            }
            if(o1.directory)
                return -1;
            
            return 1;
        }
    },
    FILENAME_DIRAFTER{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            if(o1.directory == o2.directory){
                return FILENAME.compare(o1, o2);
            }
            if(o1.directory)
                return 1;
            
            return -1;
        }
    },
    SIZE{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return Long.compare(o1.size, o2.size);
        }
    },
    DATE{
        @Override
        public int compare(SMBFileStruct o1, SMBFileStruct o2) {
            return o1.lastModified.compareTo(o2.lastModified);
        }
    },
    F;
    
//    public abstract Comparator<> getComparator

    @Override
    public int compare(SMBFileStruct o1, SMBFileStruct o2) {
        return 1;
    }
    
}
