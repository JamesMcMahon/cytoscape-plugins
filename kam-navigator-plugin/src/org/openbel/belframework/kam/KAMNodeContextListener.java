/*
 * KAM Navigator Plugin
 *
 * URLs: http://openbel.org/
 * Copyright (C) 2012, Selventa
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openbel.belframework.kam;

import static com.selventa.belframework.ws.client.EdgeDirectionType.BOTH;
import static com.selventa.belframework.ws.client.EdgeDirectionType.FORWARD;
import static com.selventa.belframework.ws.client.EdgeDirectionType.REVERSE;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_FUNCTION_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_ID_ATTR;
import static org.openbel.belframework.kam.KAMNavigatorPlugin.KAM_NODE_LABEL_ATTR;
import giny.view.NodeView;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.EdgeDirectionType;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import ding.view.NodeContextMenuListener;

/**
 * {@link KAMNodeContextListener} contributes actions to the context-sensitive
 * menu when clicking on a {@link CyNode cytoscape node}.  Specifically the
 * user is allowed to:
 * <ul>
 * <li>Expand to {@link KamNode kam nodes} downstream of current node.</li>
 * <li>Expand to {@link KamNode kam nodes} upstream of current node.</li>
 * <li>Expand in both directions from the {@link KamNode kam nodes}.</li>
 * </ul>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KAMNodeContextListener implements PropertyChangeListener,
        NodeContextMenuListener {
    private static final CyAttributes nodeAtt = Cytoscape.getNodeAttributes();

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e != null) {
            if (CytoscapeDesktop.NETWORK_VIEW_CREATED.equals(e
                    .getPropertyName())) {
                CyNetworkView view = (CyNetworkView) e.getNewValue();
                view.addNodeContextMenuListener(this);
            } else if (CytoscapeDesktop.NETWORK_VIEW_DESTROYED.equals(e
                    .getPropertyName())) {
                CyNetworkView view = (CyNetworkView) e.getNewValue();
                view.removeNodeContextMenuListener(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNodeContextMenuItems(NodeView nv, JPopupMenu menu) {
        // return if cynode does not reference kam node
        final CyNode cynode = (CyNode) nv.getNode();
        String cyid = cynode.getIdentifier();
        final String id = nodeAtt.getStringAttribute(cyid, KAM_NODE_ID_ATTR);
        final String func = nodeAtt.getStringAttribute(cyid, KAM_NODE_FUNCTION_ATTR);
        final String lbl = nodeAtt.getStringAttribute(cyid, KAM_NODE_LABEL_ATTR);
        if (id == null || func == null || lbl == null) {
            return;
        }

        if (menu == null) {
            menu = new JPopupMenu();
        }

        // construct node menu and add to context popup
        final JMenu kamNodeItem = new JMenu("KAM Node");
        final JMenuItem downstream = new JMenuItem(new ExpandAction(FORWARD,
                cynode, (CyNetworkView) nv.getGraphView()));
        kamNodeItem.add(downstream);
        final JMenuItem upstream = new JMenuItem(new ExpandAction(REVERSE,
                cynode, (CyNetworkView) nv.getGraphView()));
        kamNodeItem.add(upstream);
        final JMenuItem both = new JMenuItem(new ExpandAction(BOTH, cynode,
                (CyNetworkView) nv.getGraphView()));
        kamNodeItem.add(both);
        menu.add(kamNodeItem);
    }

    /**
     * A menu {@link AbstractAction action} that drives the expansion of the
     * active {@link KamNode kam node}.
     *
     * @see #actionPerformed(ActionEvent)
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static class ExpandAction extends AbstractAction {
        private static final long serialVersionUID = -8467637028387407708L;
        private final EdgeDirectionType direction;
        private final KAMService kamService;
        private final CyNode cynode;
        private final CyNetworkView view;

        private ExpandAction(final EdgeDirectionType direction,
                final CyNode cynode, final CyNetworkView view) {
            super("Expand " + getLabel(direction));
            this.direction = direction;
            this.kamService = KAMServiceFactory.getInstance().getKAMService();
            this.cynode = cynode;
            this.view = view;
        }

        private static String getLabel(final EdgeDirectionType direction) {
            switch (direction) {
                case FORWARD:
                    return "Downstream";
                case REVERSE:
                    return "Upstream";
                case BOTH:
                    return "Downstream & Upstream";
                default:
                    throw new UnsupportedOperationException("Unsupported direction");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // FIXME If a cytoscape session is restored the KAMNetwork
            // will not exist.  We will have to reconnect to the KAM.

            final CyNetwork network = view.getNetwork();
            final KAMNetwork kamNetwork = KAMSession.getInstance()
                    .getKAMNetwork(network);
            final KamNode kamNode = kamNetwork.getKAMNode(cynode);

            final Set<CyNode> nn = new HashSet<CyNode>();
            final List<KamEdge> edges = kamService.getAdjacentKamEdges(
                    kamNode, direction, null);
            for (final KamEdge edge : edges) {
                CyEdge cye = kamNetwork.addEdge(edge);

                nn.add((CyNode) cye.getSource());
                nn.add((CyNode) cye.getTarget());
            }

            // do not track the node to expand; we don't want it to re-layout
            //nn.remove(cynode);

            network.unselectAllNodes();
            network.setSelectedNodeState(nn, true);

            CyLayoutAlgorithm dcl = CyLayouts.getLayout("degree-circle");
            dcl.setSelectedOnly(true);
            dcl.doLayout(view);

            view.redrawGraph(true, true);
        }
    }
}
