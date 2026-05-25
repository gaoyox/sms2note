#!/bin/bash

keytool -genkey -v -keystore release-key.jks -storepass android -alias sms2note -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Sms2Note, OU=Development, O=Example, L=Beijing, ST=Beijing, C=CN"