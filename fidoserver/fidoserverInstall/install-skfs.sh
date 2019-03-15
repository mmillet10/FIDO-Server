#!/bin/bash
###################################################################################
# Copyright StrongAuth, Inc. All Rights Reserved.
#
# Use of this source code is governed by the Gnu Lesser General Public License 2.3.
# The license can be found at https://github.com/StrongKey/FIDO-Server/LICENSE
###################################################################################
# Uncomment to show detailed installation process
#SHOWALL=1

##########################################
##########################################
# Server Passwords
LINUX_PASSWORD=ShaZam123

XMXSIZE=512m

##########################################
##########################################

# Flags to indicate if a module should be installed
INSTALL_GLASSFISH=Y
INSTALL_FIDO=Y

# Start Required Distributables
GLASSFISH=payara-4.1.2.181.zip
# End Required Distributables

# Other vars
STRONGKEY_HOME=/usr/local/strongkey
SKFS_HOME=$STRONGKEY_HOME/skfs
GLASSFISH_HOME=$STRONGKEY_HOME/payara41/glassfish
GLASSFISH_CONFIG=$GLASSFISH_HOME/domains/domain1/config
SKFS_SOFTWARE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

function check_exists {
for ARG in "$@"
do
    if [ ! -f $ARG ]; then
        >&2 echo -e "\E[31m$ARG Not Found. Check to ensure the file exists in the proper location and try again.\E[0m"
        exit 1
    fi
done
}

function get_ip {
        # Try using getent if it is available, best option
        if ! getent hosts $1 2>/dev/null | awk '{print $1; succ=1} END{exit !succ}'; then

                # If we are here, likely don't have getent. Try reading /etc/hosts.
                if ! awk "/^[^#].*$1/ "'{ print $1; succ=1} END{exit !succ}' /etc/hosts; then

                        # Wasn't in /etc/hosts, try DNS
                        if ! dig +short +time=2 +retry=1 +tries=1 $1 | grep '.' 2>/dev/null; then

                                # Can't resolve IP
                                >&2 echo -e "\E[31mFQDN $1 not resolvable. Modify DNS or add a hosts entry and try again.\E[0m"
                                exit 1
                        fi
                fi
        fi
}

# install required packages
YUM_CMD=$(which yum  2>/dev/null)
APT_GET_CMD=$(which apt-get 2>/dev/null)

echo "Installing required linux packages ..."
if [[ ! -z $YUM_CMD ]]; then
    yum -y install wget unzip java-1.8.0-openjdk >/dev/null 2>&1
elif [[ ! -z $APT_GET_CMD ]]; then
    apt-get update >/dev/null 2>&1
    apt install wget unzip openjdk-8-jdk-headless daemon rng-tools -y >/dev/null 2>&1
else
   echo "error can't install packages"
   exit 1;
fi


# download required software
if [ ! -f $SKFS_SOFTWARE/$GLASSFISH ]; then
        echo "Downloading Payara ..."
        wget http://repo1.maven.org/maven2/fish/payara/distributions/payara/4.1.2.181/payara-4.1.2.181.zip -q
fi


# Make sure we can resolve our own hostname
get_ip "$(hostname)" > /dev/null

# Check that the script is run as root
if [ $UID -ne 0 ]; then
        >&2 echo -e "\E[31m$0 must be run as root\E[0m"
        exit 1
fi

# Check that strongkey doesn't already exist
if $(id strongkey &> /dev/null); then
        >&2 echo -e "\E[31m'strongkey' user already exists. Run cleanup.sh and try again.\E[0m"
        exit 1
fi

# Check that all files are present
if [ $INSTALL_GLASSFISH = 'Y' ]; then
        check_exists $SKFS_SOFTWARE/$GLASSFISH
fi

if [ $INSTALL_FIDO = 'Y' ]; then
        check_exists $SKFS_SOFTWARE/signingkeystore.bcfks $SKFS_SOFTWARE/signingtruststore.bcfks
fi

# Make backup directory if not there
if [ -d /etc/org ]; then
        :
else
        mkdir /etc/org
        if [ -f /etc/bashrc ]; then
                cp /etc/bashrc /etc/org
        else
                cp /etc/bash.bashrc /etc/org
        fi
        cp /etc/sudoers /etc/org
fi

# Create the strongkey group and user, and add it to /etc/sudoers
groupadd strongkey
useradd -g strongkey -c"StrongKey" -d $STRONGKEY_HOME -m strongkey
echo strongkey:$LINUX_PASSWORD | /usr/sbin/chpasswd
cat >> /etc/sudoers <<-EOFSUDOERS

## SKFS permissions
Cmnd_Alias SKFS_COMMANDS = /usr/sbin/service glassfishd start, /usr/sbin/service glassfishd stop, /usr/sbin/service glassfishd restart
strongkey ALL=SKFS_COMMANDS
EOFSUDOERS

##### Create skfsrc #####
cat > /etc/skfsrc << EOFSKFSRC
    export GLASSFISH_HOME=$GLASSFISH_HOME
   export STRONGKEY_HOME=$STRONGKEY_HOME
              export PATH=\$GLASSFISH_HOME/bin:\$STRONGKEY_HOME/bin:/usr/lib64/qt-3.3/bin:/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin:/root/bin

alias str='cd $STRONGKEY_HOME'
alias dist='cd $STRONGKEY_HOME/dist'
alias aslg='cd $GLASSFISH_HOME/domains/domain1/logs'
alias ascfg='cd $GLASSFISH_HOME/domains/domain1/config'
alias tsl='tail --follow=name $GLASSFISH_HOME/domains/domain1/logs/server.log'
alias java='java -Djavax.net.ssl.trustStore=\$STRONGKEY_HOME/certs/cacerts '
EOFSKFSRC

if [ -f /etc/bashrc ]; then
        echo ". /etc/skfsrc" >> /etc/bashrc
else
        echo ". /etc/skfsrc" >> /etc/bash.bashrc
fi

# Make needed directories
mkdir -p $STRONGKEY_HOME/certs $STRONGKEY_HOME/Desktop $STRONGKEY_HOME/dbdumps $STRONGKEY_HOME/lib $STRONGKEY_HOME/bin $STRONGKEY_HOME/appliance/etc $STRONGKEY_HOME/crypto/etc $SKFS_HOME/etc $SKFS_HOME/keystores

##### Install Fido #####
if [ $INSTALL_FIDO = 'Y' ]; then

        echo "Installing SKFS..."

        cp $SKFS_SOFTWARE/certimport.sh $STRONGKEY_HOME/bin
        cp $STRONGKEY_HOME/bin/* $STRONGKEY_HOME/Desktop/

        chmod 700 $STRONGKEY_HOME/Desktop/*.sh

        SERVICE_LDAP_USERNAME=$(sed -r 's|^[cC][nN]=([^,]*),.*|\1|' <<< "$SERVICE_LDAP_SVCUSER_DN")
        SERVICE_LDAP_SUFFIX=$(sed -r 's|^[cC][nN]=[^,]*(,.*)|\1|' <<< "$SERVICE_LDAP_SVCUSER_DN")

        SERVICE_LDAP_PINGUSER=$(sed -r 's|^[cC][nN]=([^,]*),.*|\1|' <<< "$SERVICE_LDAP_PINGUSER_DN")
        SERVICE_LDAP_PINGUSER_SUFFIX=$(sed -r 's|^[cC][nN]=[^,]*(,.*)|\1|' <<< "$SERVICE_LDAP_PINGUSER_DN")

        if [ "${SERVICE_LDAP_SUFFIX}" != "${SERVICE_LDAP_PINGUSER_SUFFIX}" ]; then
                echo "Warning: SERVICE_LDAP_USER and SERVICE_LDAP_PINGUSER must be in the same OU. Pinguser may not authenticate as expected. Run update-ldap-config with corrected users."
        fi

        cp $SKFS_SOFTWARE/signingkeystore.bcfks $SKFS_SOFTWARE/signingtruststore.bcfks $SKFS_HOME/keystores

fi

##### Payara #####
if [ $INSTALL_GLASSFISH = 'Y' ]; then
        echo "Installing Payara..."
        if [ $SHOWALL ]; then
                unzip $SKFS_SOFTWARE/$GLASSFISH -d $STRONGKEY_HOME
        else
                unzip $SKFS_SOFTWARE/$GLASSFISH -d $STRONGKEY_HOME > /dev/null
        fi

        if [ -d /root/.gfclient ]; then
                rm -rf /root/.gfclient
        fi

        if [ -d $STRONGKEY_HOME/.gfclient ]; then
                rm -rf $STRONGKEY_HOME/.gfclient
        fi

        cp $SKFS_SOFTWARE/glassfishd /etc/init.d
        chmod 755 /etc/init.d/glassfishd
        /lib/systemd/systemd-sysv-install enable glassfishd

        keytool -genkeypair -alias skfs -keystore $GLASSFISH_CONFIG/keystore.jks -storepass changeit -keypass changeit -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 3562 -dname "CN=$(hostname),OU=\"StrongKey FidoServer\"" &>/dev/null
        keytool -changealias -alias s1as -destalias s1as.original -keystore $GLASSFISH_CONFIG/keystore.jks -storepass changeit &>/dev/null
        keytool -changealias -alias skfs -destalias s1as -keystore $GLASSFISH_CONFIG/keystore.jks -storepass changeit &>/dev/null
        sed -ri 's|^(com.sun.enterprise.server.logging.GFFileHandler.rotationOnDateChange=).*|\1true|
                 s|^(com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes=).*|\1200000000|' $GLASSFISH_CONFIG/logging.properties
        keytool -exportcert -alias s1as -file $STRONGKEY_HOME/certs/$(hostname).der --keystore $GLASSFISH_CONFIG/keystore.jks -storepass changeit &>/dev/null
        keytool -importcert -noprompt -alias $(hostname) -file $STRONGKEY_HOME/certs/$(hostname).der --keystore $STRONGKEY_HOME/certs/cacerts -storepass changeit &>/dev/null
        keytool -importcert -noprompt -alias $(hostname) -file $STRONGKEY_HOME/certs/$(hostname).der --keystore $GLASSFISH_CONFIG/cacerts.jks -storepass changeit &>/dev/null
fi


##### Change ownership of files #####
chown -R strongkey:strongkey $STRONGKEY_HOME

##### Start Payara #####
touch $STRONGKEY_HOME/crypto/etc/crypto-configuration.properties

echo "appliance.cfg.property.serverid=1" > $STRONGKEY_HOME/appliance/etc/appliance-configuration.properties
echo "appliance.cfg.property.enableddomains.ccspin=$CCS_DOMAINS" >> $STRONGKEY_HOME/appliance/etc/appliance-configuration.properties
echo "appliance.cfg.property.replicate=false" >> $STRONGKEY_HOME/appliance/etc/appliance-configuration.properties
chown -R strongkey $STRONGKEY_HOME/appliance

chown strongkey:strongkey $STRONGKEY_HOME/crypto/etc/crypto-configuration.properties
service glassfishd start
sleep 10

##### Perform Payara Tasks #####
$GLASSFISH_HOME/bin/asadmin set server.network-config.network-listeners.network-listener.http-listener-1.enabled=false
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.http.request-timeout-seconds=7200
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.ssl.ssl3-tls-ciphers=+TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,+TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,+TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,+TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,+TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,+TLS_DHE_RSA_WITH_AES_256_CBC_SHA
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.ssl.ssl2-enabled=false
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.ssl.ssl3-enabled=false
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.ssl.tls-enabled=false
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.ssl.tls11-enabled=false
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.http.trace-enabled=false
$GLASSFISH_HOME/bin/asadmin set server.network-config.protocols.protocol.http-listener-2.http.xpowered-by=false
$GLASSFISH_HOME/bin/asadmin set server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=1000
$GLASSFISH_HOME/bin/asadmin set server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=10

$GLASSFISH_HOME/bin/asadmin delete-jvm-options $($GLASSFISH_HOME/bin/asadmin list-jvm-options | sed -n '/\(-XX:NewRatio\|-XX:MaxPermSize\|-XX:PermSize\|-client\|-Xmx\|-Xms\)/p' | sed 's|:|\\\\:|' | tr '\n' ':')
$GLASSFISH_HOME/bin/asadmin create-jvm-options -Djtss.tcs.ini.file=$STRONGKEY_HOME/lib/jtss_tcs.ini:-Djtss.tsp.ini.file=$STRONGKEY_HOME/lib/jtss_tsp.ini:-Xmx${XMXSIZE}:-Xms${XMXSIZE}:-server:-Djdk.tls.ephemeralDHKeySize=2048:-Dproduct.name="":-XX\\:-DisableExplicitGC

cat > $GLASSFISH_HOME/domains/domain1/docroot/app.json << EOFAPPJSON
{
  "trustedFacets" : [{
    "version": { "major": 1, "minor" : 0 },
    "ids": [
           "https://$(hostname)",
           "https://$(hostname):8181"
    ]
  }]
}
EOFAPPJSON

chown strongkey $GLASSFISH_HOME/domains/domain1/docroot/app.json

echo "Deploying StrongKey FidoServer ..."
$GLASSFISH_HOME/bin/asadmin deploy $SKFS_SOFTWARE/fidoserver.ear

echo "Done!"

