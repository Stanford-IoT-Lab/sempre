CLASS_PATH = ../classes
JNIPATH ?= /usr/lib/jvm/java-1.8.0/include/
JNIPATH_MD ?= /usr/lib/jvm/java-1.8.0/include/linux

all : libposix.so

# $* matches the target filename without the extension
posix.h : $(CLASS_PATH)/edu/stanford/nlp/sempre/PosixHelper.class
	javah -classpath $(CLASS_PATH) -o $@ edu.stanford.nlp.sempre.PosixHelper
posix.o : posix.h

CPPFLAGS = -I $(JNIPATH) -I $(JNIPATH_MD)
CXXFLAGS = -Wall -O2 -g

libposix_OBJECTS = posix.o

libposix.so : $(libposix_OBJECTS)
	g++ -shared -o $@ $(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS) $<

%.o : %.cpp
	g++ $(CPPFLAGS) $(CXXFLAGS) -fPIC -DPIC -c $< -o $@

clean :
	rm libposix.so $(libposix_OBJECTS)