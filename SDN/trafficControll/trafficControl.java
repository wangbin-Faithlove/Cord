package org.onosproject.amon;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;


import java.util.ArrayList;
import java.util.Collections;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
public class TrafficControl {

    private static final int DEFAULT_TIMEOUT = 1;
    // tc队列id
    private static final long QUEUE1_ID = 16L;
    private static final long QUEUE2_ID = 17L;
    private static final long QUEUE3_ID = 18L;
    private static final long QUEUE4_ID = 19L;
    private static final long QUEUE5_ID = 20L;


    //host 通过下发流表的优先级来来区分主机,优先级分别为ip值
    private int prefix = IPv4.toIPv4Address("10.0.0.0");
    private int hostIp1 = IPv4.toIPv4Address("10.0.0.1");
    private int hostIp2 = IPv4.toIPv4Address("10.0.0.2");
    private int hostIp3 = IPv4.toIPv4Address("10.0.0.3");
    private int hostIp4 = IPv4.toIPv4Address("10.0.0.4");
    private int hostIp5 = IPv4.toIPv4Address("10.0.0.5");


    private final Logger log = getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private final FlowRuleListener flowRuleListener = new InternalFlowRuleListener();
    private ApplicationId appId;
    private TrafficControlProcessor processor = new TrafficControlProcessor();

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.trafficCtl");
        packetService.addProcessor(processor, PacketProcessor.director(2));
        flowRuleService.addListener(flowRuleListener);
        requestIntercepts();

        log.info("Started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        withdrawIntercepts();
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        flowRuleService.removeListener(flowRuleListener);
        processor = null;
        log.info("Stopped");
    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private class TrafficControlProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }
            if (isControlPacket(ethPkt)) {
                return;
            }
            HostId id = HostId.hostId(ethPkt.getDestinationMAC());
            Host dst = hostService.getHost(id);

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                if (id.mac().isMulticast()) {
                    return;
                }
            }
            if (id.mac().isLinkLocal()) {
                return;
            }
            if (dst == null) {
                flood(context);
                return;
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) pkt.parsed().getPayload();
                int destIp = ipv4Packet.getDestinationAddress();
                if (destIp == hostIp5) {
                    installRule(context, dst.location().port());
                } else {
                    installRuleH5(context, dst.location().port());
                }

            }

        }
    }

    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    private void flood(PacketContext context) {
        log.info("泛洪\n");
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                context.inPacket().receivedFrom())) {
            context.treatmentBuilder().setOutput(PortNumber.FLOOD);
            context.send();
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, long queueId, PortNumber portNumber) {
        context.treatmentBuilder().setQueue(queueId, portNumber).setOutput(portNumber);
        context.send();
    }

    private void installRule(PacketContext context, PortNumber portNumber) {
        int hostNumber;
        int myFlowRule = 0;

        for (FlowRule flowrule : flowRuleService.getFlowRulesById(appId)) {
            if (flowrule.priority() < 105 && flowrule.priority() > 100) {
                myFlowRule++;
            }
        }
        hostNumber = myFlowRule + 1;

        log.info("主机数{}", hostNumber);
        Ethernet inPkt = context.inPacket().parsed();
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            log.info("arp包");
            context.treatmentBuilder().setOutput(portNumber);
            context.send();
        }

        if (inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
            int srcIp = ipv4Packet.getSourceAddress();

            if (hostNumber == 1) {
                flowRuleService.applyFlowRules(setFlowRule(srcIp, portNumber, QUEUE1_ID, deviceId));
            } else if (hostNumber == 2) {
                for (FlowRule flowrule : flowRuleService.getFlowRulesById(appId)) {
                    if (flowrule.priority() < 105 && flowrule.priority() > 100) {
                        if (flowrule.priority() > (srcIp - prefix + 100)) {
                            flowRuleService.applyFlowRules(setFlowRule(srcIp, portNumber, QUEUE1_ID, deviceId),
                                    setFlowRule(flowrule.priority() + prefix - 100,
                                            portNumber, QUEUE2_ID, deviceId));
                            packetOut(context, QUEUE1_ID, portNumber);
                        } else {
                            flowRuleService.applyFlowRules(setFlowRule(srcIp, portNumber, QUEUE2_ID, deviceId),
                                    setFlowRule(flowrule.priority() + prefix - 100, portNumber, QUEUE1_ID, deviceId));
                            packetOut(context, QUEUE1_ID, portNumber);
                        }
                    }
                }
            } else if (hostNumber == 3) {
                int max = srcIp - prefix + 100;
                int min = 0;
                int med = 0;
                int tmp;
                for (FlowRule flowrule : flowRuleService.getFlowRulesById(appId)) {
                    if (flowrule.priority() < 105 && flowrule.priority() > 100) {
                        if (flowrule.priority() > max) {
                            tmp = max;
                            max = flowrule.priority();
                            min = med;
                            med = tmp;
                        } else if (flowrule.priority() > med) {
                            tmp = med;
                            med = flowrule.priority();
                            min = tmp;
                        } else {
                            min = flowrule.priority();
                        }
                    }
                }
                flowRuleService.applyFlowRules(setFlowRule(min + prefix - 100, portNumber, QUEUE2_ID, deviceId),
                        setFlowRule(med + prefix - 100, portNumber, QUEUE3_ID, deviceId),
                        setFlowRule(max + prefix - 100, portNumber, QUEUE4_ID, deviceId));
                packetOut(context, QUEUE2_ID, portNumber);
            } else if (hostNumber >= 4) {
                flowRuleService.applyFlowRules(setFlowRule(hostIp1, portNumber, QUEUE2_ID, deviceId),
                        setFlowRule(hostIp2, portNumber, QUEUE3_ID, deviceId),
                        setFlowRule(hostIp3, portNumber, QUEUE4_ID, deviceId),
                        setFlowRule(hostIp4, portNumber, QUEUE5_ID, deviceId));
                packetOut(context, QUEUE2_ID, portNumber);
            }
        }
    }

    private FlowRule setFlowRule(int hostip, PortNumber portNumber, long queueId, DeviceId deviceId) {
        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        Ip4Prefix matchIpv4SrcPrefix = Ip4Prefix.valueOf(hostip, Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIpv4DstPrefix = Ip4Prefix.valueOf(hostIp5, Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(matchIpv4SrcPrefix)
                .matchIPDst(matchIpv4DstPrefix);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setQueue(queueId)
                .setOutput(portNumber)
                .build();

        return flowRuleBuilder.withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(hostip - prefix + 100)
                .fromApp(appId)
                .makeTemporary(DEFAULT_TIMEOUT)
                .forDevice(deviceId)
                .build();

    }

    //H5->H1,2,3,4,5
    private void packetOutH5(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    private void installRuleH5(PacketContext context, PortNumber portNumber) {
        Ethernet inPkt = context.inPacket().parsed();
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            packetOutH5(context, portNumber);
        }

        if (inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
            Ip4Prefix matchIpv4SrcPrefix = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                    Ip4Prefix.MAX_MASK_LENGTH);
            Ip4Prefix matchIpv4DstPrefix = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                    Ip4Prefix.MAX_MASK_LENGTH);
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(matchIpv4SrcPrefix)
                    .matchIPDst(matchIpv4DstPrefix);
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber).build();

        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();
        FlowRule flowRule = flowRuleBuilder.withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(10)
                .fromApp(appId)
                .makeTemporary(10)
                .forDevice(deviceId)
                .build();
        flowRuleService.applyFlowRules(flowRule);
        packetOutH5(context, portNumber);
    }

    //监听器

    private class InternalFlowRuleListener implements FlowRuleListener {

        @Override
        public void event(FlowRuleEvent event) {
            if (event.type() == FlowRuleEvent.Type.RULE_REMOVED) {
                DeviceId deviceId = DeviceId.NONE;
                ArrayList<Integer> prio = new ArrayList<Integer>();
                for (FlowRule flowrule : flowRuleService.getFlowRulesById(appId)) {
                    if (flowrule.priority() < 105 && flowrule.priority() > 100) {
                        //flowRuleService.removeFlowRules(flowrule);
                        deviceId = flowrule.deviceId();
                        prio.add(flowrule.priority());
                    }
                }

                Collections.sort(prio);
                int ruleNum = prio.size();
                log.info("减少主机后,有多少流表{}", ruleNum);

                if (ruleNum == 0 || ruleNum == 1 || ruleNum == 4) {
                    return;
                }
                if (ruleNum == 2) {
                    flowRuleService.applyFlowRules(setFlowRule(prio.get(0) + prefix - 100,
                            PortNumber.portNumber(5), QUEUE1_ID, deviceId),
                            setFlowRule(prio.get(1) + prefix - 100, PortNumber.portNumber(5),
                                    QUEUE2_ID, deviceId));

                }
                if (ruleNum == 3) {
                    flowRuleService.applyFlowRules(setFlowRule(prio.get(0) + prefix - 100,
                            PortNumber.portNumber(5), QUEUE2_ID, deviceId),
                            setFlowRule(prio.get(1) + prefix - 100, PortNumber.portNumber(5),
                                    QUEUE3_ID, deviceId),
                            setFlowRule(prio.get(2) + prefix - 100, PortNumber.portNumber(5),
                                    QUEUE4_ID, deviceId));
                }
            }
        }
    }
}
