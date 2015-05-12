
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
				version="1.0">
	<xsl:template match="/">
		<xml>
			<records>
				<record>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='type' and not(@qualifier)]">
						<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='type' and not(@qualifier)]" />
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
						<contributors>
							<authors>
								<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']" />
							</authors>
						</contributors>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='title' and not(@qualifier='chapter')] or //dspace:field[@mdschema='dc' and @element='source' and not(@qualifier)]">
						<titles>
							<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='title']" />
							<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='source' and not(@qualifier)]" mode="title" />
							<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']" />
						</titles>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='source' and not(@qualifier)]">
						<periodical>
							<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='source' and not(@qualifier)]" mode="periodical" />
						</periodical>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='citationpages']">
						<pages><xsl:value-of select="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='citationpages']" /></pages>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='startpage'] or //dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='lastpage']">
						<xsl:choose>
							<xsl:when test="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='startpage'] and //dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='lastpage']">
								<pages><xsl:value-of select="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='startpage']"/>-<xsl:value-of select="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='lastpage']"/></pages>
							</xsl:when>
							<xsl:when test="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='startpage']">
								<pages><xsl:value-of select="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='startpage']" /></pages>
							</xsl:when>
							<xsl:otherwise>
								<pages><xsl:value-of select="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='endpage']" /></pages>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='citationvolume']">
						<volume><xsl:value-of select="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='citationvolume']" /></volume>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='citationnumber']">
						<number><xsl:value-of select="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='citationnumber']" /></number>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='issue']">
						<number><xsl:value-of select="//dspace:field[@mdschema='local' and @element='bibliographicCitation' and @qualifier='issue']" /></number>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='subject']">
						<keywords>
							<xsl:apply-templates select="//dspace:field[@mdschema='dc' and @element='subject']" />
						</keywords>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued']">
						<dates>
							<year><xsl:value-of select="substring(//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued'],1,4)" /></year>
						</dates>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='publisher' and not(@qualifier)]">
						<publisher><xsl:value-of select="//dspace:field[@mdschema='dc' and @element='publisher']" /></publisher>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='isbn']">
						<isbn><xsl:value-of select="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='isbn']" /></isbn>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']">
						<isbn><xsl:value-of select="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='issn']" /></isbn>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='doi']">
						<electronic-resource-num><xsl:value-of select="//dspace:field[@mdschema='local' and @element='identifier' and @qualifier='doi']" /></electronic-resource-num>
					</xsl:if>
					<xsl:if test="//dspace:field[@mdschema='dc' and @element='description' and @qualifier='abstract']">
						<abstract><xsl:value-of select="//dspace:field[@mdschema='dc' and @element='description' and @qualifier='abstract']" /></abstract>
					</xsl:if>
				</record>
			</records>
		</xml>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='contributor' and @qualifier='author']">
		<author><xsl:value-of select="." /></author>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='title']">
		<xsl:choose>
			<xsl:when test="@qualifier='alternate'">
				<alt-title><xsl:value-of select="." /></alt-title>
			</xsl:when>
			<xsl:otherwise>
				<title><xsl:value-of select="." /></title>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='source' and not(@qualifier)]" mode="periodical">
		<full-title><xsl:value-of select="." /></full-title>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='source' and not(@qualifier)]" mode="title">
		<secondary-title><xsl:value-of select="." /></secondary-title>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='relation' and @qualifier='ispartof']">
		<xsl:variable name="ispartof" select="." />
		<xsl:if test="//dspace:field[@mdschema='dc' and @element='type' and not(@qualifier)] = 'Book chapter'">
			<secondary-title><xsl:value-of select="$ispartof" /></secondary-title>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='subject']">
		<keyword><xsl:value-of select="." /></keyword>
	</xsl:template>
	
	<xsl:template match="//dspace:field[@mdschema='dc' and @element='type' and not(@qualifier)]">
		<xsl:variable name="refType" select="." />
		<xsl:choose>
			<xsl:when test="$refType='Journal article' or $refType='Journal Article' or $refType='Article'">
				<ref-type name="Journal Article">17</ref-type>
			</xsl:when>
			<xsl:when test="contains($refType,'Thesis') or contains($refType, 'thesis')">
				<ref-type name="Thesis">34</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Image' or $refType='photographic prints' or $refType='Cartoon' or $refType='cartoon' or $refType='Still Image' or $refType='Creative work'">
				<ref-type name="Artwork">2</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Book chapter' or $refType='Book Chapter'">
				<ref-type name="Book Section">5</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Catographic material'">
				<ref-type name="Map">20</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Conference paper'">
				<ref-type name="Conference Paper">47</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Legal submission'">
				<ref-type name="Case">7</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Recording, oral' or $refType='Sound' or $refType='Moving image' or $refType='Video' or $refType='audio' or $refType='Multimedia'">
				<ref-type name="Audiovisual material">3</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Report'">
				<ref-type name="Report">27</ref-type>
			</xsl:when>
			<xsl:when test="$refType='Book'">
				<ref-type name="Book">6</ref-type>
			</xsl:when>
			<xsl:otherwise>
				<ref-type name="Generic">13</ref-type>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>