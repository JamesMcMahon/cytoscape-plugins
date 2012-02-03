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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.openbel.belframework.webservice.KAMService;
import org.openbel.belframework.webservice.KAMServiceFactory;

import com.selventa.belframework.ws.client.Annotation;
import com.selventa.belframework.ws.client.BelStatement;
import com.selventa.belframework.ws.client.BelTerm;
import com.selventa.belframework.ws.client.Citation;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.KamNode;

import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.view.cytopanels.CytoPanelImp;

/**
 * {@link DetailsView} provides the UI to show kam node/edge info in the result
 * panel of cytoscape.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class DetailsView {
    private static DetailsView instance;
    private final KAMService kamService;
    private final CytoPanelImp resultsPanel;
    private final JPanel nodeDetailPanel;
    private final JPanel edgeDetailPanel;
    private final TermTableModel termTableModel;
    private final StatementTableModel stmtTableModel;
    private final AnnotationTableModel annTableModel;
    private final CitationTableModel citTableModel;

    public static DetailsView getInstance() {
        if (instance == null) {
            instance = new DetailsView();
        }

        return instance;
    }

    private DetailsView() {
        resultsPanel = (CytoPanelImp) Cytoscape.getDesktop()
                .getCytoPanel(SwingConstants.EAST);
        this.kamService = KAMServiceFactory.getInstance().getKAMService();

        // build node details panel and add to results panel
        nodeDetailPanel = new JPanel();
        nodeDetailPanel.setName("KAM Node Info");

        JScrollPane tblScroll = new JScrollPane();
        JTable termTbl = new JTable();
        termTbl.setShowGrid(true);
        termTbl.setShowHorizontalLines(true);
        termTbl.setShowVerticalLines(true);

        nodeDetailPanel.setLayout(new BorderLayout());

        JLabel nodeLbl = new JLabel("Supporting BEL Terms");
        nodeDetailPanel.add(nodeLbl, BorderLayout.NORTH);

        termTableModel = new TermTableModel();
        termTbl.setModel(termTableModel);
        tblScroll.setViewportView(termTbl);

        nodeDetailPanel.add(tblScroll, BorderLayout.CENTER);
        resultsPanel.add(nodeDetailPanel);

        // build edge details panel and add to results panel
        edgeDetailPanel = new JPanel(new GridLayout(3, 1));
        edgeDetailPanel.setName("KAM Edge Info");

        TextAreaCellRenderer textRenderer = new TextAreaCellRenderer();

        // statement panel
        JPanel stmtPanel = new JPanel();
        stmtPanel.setLayout(new BorderLayout());
        JLabel edgeLbl = new JLabel("Supporting BEL Statements");
        stmtPanel.add(edgeLbl, BorderLayout.NORTH);
        JScrollPane stmtScroll = new JScrollPane();
        JTable stmtTbl = new JTable();
        stmtTbl.setShowGrid(true);
        stmtTbl.setShowHorizontalLines(true);
        stmtTbl.setShowVerticalLines(true);
        stmtTableModel = new StatementTableModel();
        stmtTbl.setModel(stmtTableModel);
        stmtTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stmtTbl.getSelectionModel().addListSelectionListener(
                new StatementSelectionListener());
        stmtScroll.setViewportView(stmtTbl);
        stmtPanel.add(stmtScroll, BorderLayout.CENTER);
        edgeDetailPanel.add(stmtPanel);

        // annotation panel
        JPanel annPanel = new JPanel();
        annPanel.setLayout(new BorderLayout());
        JLabel annLbl = new JLabel("Statement Annotations");
        annPanel.add(annLbl, BorderLayout.NORTH);
        JScrollPane annScroll = new JScrollPane();
        JTable annTbl = new JTable();
        annTbl.setShowGrid(true);
        annTbl.setShowHorizontalLines(true);
        annTbl.setShowVerticalLines(true);
        annTableModel = new AnnotationTableModel();
        annTbl.setModel(annTableModel);
        annTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        annTbl.getColumnModel().getColumn(1).setCellRenderer(textRenderer);

        annScroll.setViewportView(annTbl);
        annPanel.add(annScroll, BorderLayout.CENTER);
        edgeDetailPanel.add(annPanel);

        // citation panel
        JPanel citPanel = new JPanel();
        citPanel.setLayout(new BorderLayout());
        JLabel citLbl = new JLabel("Citation");
        citPanel.add(citLbl, BorderLayout.NORTH);
        JScrollPane citScroll = new JScrollPane();
        JTable citTbl = new JTable();
        citTbl.setShowGrid(true);
        citTbl.setShowHorizontalLines(true);
        citTbl.setShowVerticalLines(true);
        citTableModel = new CitationTableModel();
        citTbl.setModel(citTableModel);
        citTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        citTbl.getColumnModel().getColumn(0).setCellRenderer(textRenderer);
        citTbl.getColumnModel().getColumn(1).setCellRenderer(textRenderer);
        citTbl.getColumnModel().getColumn(2).setCellRenderer(textRenderer);
        citTbl.getColumnModel().getColumn(3).setCellRenderer(textRenderer);
        citScroll.setViewportView(citTbl);
        citPanel.add(citScroll, BorderLayout.CENTER);
        edgeDetailPanel.add(citPanel);

        // add edge details to Results Panel (cytoscape)
        resultsPanel.add(edgeDetailPanel);

        // delay showing panels
        nodeDetailPanel.setVisible(false);
        edgeDetailPanel.setVisible(false);
    }

    /**
     * Callback to handle showing node details for the active cytoscape node.
     *
     * @see NetworkDetailsListener#onSelectEvent(cytoscape.data.SelectEvent)
     * @param kn the {@link KAMNetwork kam network} containing the node
     * @param node the {@link CyNode cytoscape node} that is active
     */
    public void showNodeDetails(final KAMNetwork kn, final CyNode node) {
        final KamNode kamNode = kn.getKAMNode(node);

        final List<BelTerm> terms = kamService.getSupportingTerms(kamNode);

        // set node details and show results panel
        termTableModel.setTerms(terms);

        // show node details panel
        int nodeTabIdx = resultsPanel.indexOfComponent(nodeDetailPanel);
        resultsPanel.setSelectedIndex(nodeTabIdx);
        nodeDetailPanel.setVisible(true);
    }

    /**
     * Callback to handle showing edge details for the active cytoscape edge.
     *
     * @see NetworkDetailsListener#onSelectEvent(cytoscape.data.SelectEvent)
     * @param kn the {@link KAMNetwork kam network} containing the edge
     * @param edge the {@link CyEdge cytoscape edge} that is active
     */
    public void showEdgeDetails(final KAMNetwork kn, final CyEdge edge) {
        final KamEdge kamEdge = kn.getKAMEdge(edge);

        final List<BelStatement> statements = kamService
                .getSupportingEvidence(kamEdge);

        // set edge details and show results panel
        stmtTableModel.setStatements(statements);

        // show edge details panel
        int edgeTabIdx = resultsPanel.indexOfComponent(edgeDetailPanel);
        resultsPanel.setSelectedIndex(edgeTabIdx);
        edgeDetailPanel.setVisible(true);
    }

    /**
     * The {@link AbstractTableModel table model} for the BEL terms of the
     * active node.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class TermTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 3869233363790339464L;
        private final String[] headers = new String[] { "BEL Term" };
        private final List<BelTerm> terms;

        private TermTableModel() {
            this.terms = new ArrayList<BelTerm>();
        }

        private TermTableModel(final List<BelTerm> terms) {
            this.terms = new ArrayList<BelTerm>();
            setTerms(terms);
        }

        private void setTerms(final List<BelTerm> terms) {
            this.terms.clear();
            this.terms.addAll(terms);
            fireTableDataChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return headers.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int ci) {
            return headers[ci];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return terms.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            final BelTerm term = terms.get(ri);

            switch (ci) {
                case 0:
                    return term.getLabel();
            }

            return null;
        }
    }

    /**
     * The {@link AbstractTableModel table model} for the BEL statements of the
     * active edge.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class StatementTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 4239815126171784622L;
        private final String[] headers = new String[] { "Subject", "Relationship", "Object" };
        private final List<BelStatement> statements;

        private StatementTableModel() {
            this.statements = new ArrayList<BelStatement>();
        }

        private StatementTableModel(final List<BelStatement> statements) {
            this.statements = new ArrayList<BelStatement>();
            setStatements(statements);
        }

        private void setStatements(final List<BelStatement> statements) {
            this.statements.clear();
            this.statements.addAll(statements);

            // loaded new statements so reset statement annotations and citations
            annTableModel.setAnnotations(new ArrayList<Annotation>());
            citTableModel.setCitations(new ArrayList<Citation>());

            fireTableDataChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return headers.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int ci) {
            return headers[ci];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return statements.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            final BelStatement stmt = statements.get(ri);

            switch (ci) {
                case 0:
                    return stmt.getSubjectTerm().getLabel();
                case 1:
                    // can return null
                    return stmt.getRelationship();
                case 2:
                    final BelTerm objTerm = stmt.getObjectTerm();
                    final BelStatement objStmt = stmt.getObjectStatement();

                    if (objTerm != null) {
                        return objTerm.getLabel();
                    } else if (objStmt != null) {
                        if (objStmt.getSubjectTerm() == null
                            || objStmt.getRelationship() == null
                            || objStmt.getObjectTerm() == null) {
                            return null;
                        }

                        return objStmt.getSubjectTerm().getLabel() + " "
                                + objStmt.getRelationship() + " "
                                + objStmt.getObjectTerm().getLabel();
                    }
            }

            return null;
        }
    }

    /**
     * The {@link AbstractTableModel table model} for citations that display
     * for the selected BEL statement in the
     * {@link StatementTableModel statement table model}.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class CitationTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -7502992026898866459L;
        private final String[] headers = new String[] { "Id", "Name", "Publication Date", "Authors" };
        private final List<Citation> citations;

        private CitationTableModel() {
            this.citations = new ArrayList<Citation>();
        }

        private CitationTableModel(final List<Citation> annotations) {
            this.citations = new ArrayList<Citation>();
            setCitations(annotations);
        }

        private void setCitations(final List<Citation> citations) {
            this.citations.clear();
            this.citations.addAll(citations);
            fireTableDataChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return headers.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int ci) {
            return headers[ci];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return citations.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            final Citation cit = citations.get(ri);

            switch (ci) {
                case 0:
                    return cit.getCitationType() + " - " + cit.getId();
                case 1:
                    return cit.getName();
                case 2:
                    return cit.getPublicationDate() == null ? null : cit.getPublicationDate().toString();
                case 3:
                    return  cit.getAuthors();
            }

            return null;
        }
    }

    /**
     * The {@link AbstractTableModel table model} for annotations that display
     * for the selected BEL statement in the
     * {@link StatementTableModel statement table model}.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class AnnotationTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -2058889152696174033L;
        private final String[] headers = new String[] { "Name", "Value" };
        private final List<Annotation> annotations;

        private AnnotationTableModel() {
            this.annotations = new ArrayList<Annotation>();
        }

        private AnnotationTableModel(final List<Annotation> annotations) {
            this.annotations = new ArrayList<Annotation>();
            setAnnotations(annotations);
        }

        private void setAnnotations(final List<Annotation> annotations) {
            this.annotations.clear();
            this.annotations.addAll(annotations);
            fireTableDataChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return headers.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int ci) {
            return headers[ci];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return annotations.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            final Annotation ann = annotations.get(ri);

            switch (ci) {
                case 0:
                    return ann.getAnnotationType().getName();
                case 1:
                    // can return null
                    return ann.getValue();
            }

            return null;
        }
    }

    /**
     * The selection listener for the
     * {@link StatementTableModel statement table model} that determines the
     * selected {@link BelStatement statement} and refreshes the
     * {@link CitationTableModel citation table model} and the
     * {@link AnnotationTableModel annotation table model}.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private class StatementSelectionListener implements ListSelectionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void valueChanged(ListSelectionEvent e) {
            final ListSelectionModel selModel = (ListSelectionModel) e.getSource();

            int selectedIndex = selModel.getMinSelectionIndex();

            if (selectedIndex != -1) {
                final List<BelStatement> stmts = stmtTableModel.statements;
                final BelStatement selected = stmts.get(selectedIndex);
                annTableModel.setAnnotations(selected.getAnnotations());

                final Citation citation = selected.getCitation();
                citTableModel.setCitations(Arrays.asList(new Citation[] {citation}));
            }
        }
    }
}
