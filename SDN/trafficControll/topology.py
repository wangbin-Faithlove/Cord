"""Custom topology example

Two directly connected switches plus a host for each switch:

   host --- switch --- switch --- host

Adding the 'topos' dict with a key/value pair to generate our newly defined
topology enables one to pass in '--topo=mytopo' from the command line.
"""

from mininet.topo import Topo
from mininet.net import Mininet 
from mininet.node import CPULimitedHost, Controller, RemoteController 
from mininet.cli import CLI 
from mininet.link import TCLink, Link, Intf 
from mininet.util import dumpNodeConnections, dumpNetConnections 
from mininet.log import setLogLevel, info 

class MyTopo( Topo ):
    "Simple topology example."

    def __init__( self ):
        "Create custom topo."

        # Initialize topology
        Topo.__init__( self )

        # Add hosts and switches
        node1 = self.addHost( 'h1', ip='10.0.0.1', mac='00:00:00:00:00:01' ) 
        node2 = self.addHost( 'h2', ip='10.0.0.2', mac='00:00:00:00:00:02' ) 
        node3 = self.addHost( 'h3', ip='10.0.0.3', mac='00:00:00:00:00:03' ) 
        node4 = self.addHost( 'h4', ip='10.0.0.4', mac='00:00:00:00:00:04' ) 
        node5 = self.addHost( 'h5', ip='10.0.0.5', mac='00:00:00:00:00:05' ) 
        switch = self.addSwitch( 's1' )

        # Add links
        self.addLink( node1, switch )
        self.addLink( node2, switch )
        self.addLink( node3, switch )
        self.addLink( node4, switch )
        self.addLink( switch, node5 )


topos = { 'mytopo': ( lambda: MyTopo() ) }
