import React from 'react';
import {
  Card, Row, Col, Typography, Button, Tag, Statistic, Alert, Divider, Space
} from 'antd';
import {
  ArrowLeftOutlined, DollarOutlined, SafetyCertificateOutlined, InfoCircleOutlined
} from '@ant-design/icons';
import { AssessResponse, RiskTier } from '../types';
import RiskBreakdownCard from '../components/RiskBreakdownCard';

const { Title, Text, Paragraph } = Typography;

interface QuoteResultProps {
  result: AssessResponse;
  onReset: () => void;
}

const TIER_COLOR: Record<RiskTier, string> = {
  LOW: 'green',
  MEDIUM: 'gold',
  HIGH: 'orange',
  VERY_HIGH: 'red',
};

const TIER_LABEL: Record<RiskTier, string> = {
  LOW: 'Low Risk',
  MEDIUM: 'Medium Risk',
  HIGH: 'High Risk',
  VERY_HIGH: 'Very High Risk',
};

const QuoteResult: React.FC<QuoteResultProps> = ({ result, onReset }) => {
  const tier = result.riskTier as RiskTier;

  return (
    <div>
      <Button icon={<ArrowLeftOutlined />} onClick={onReset} style={{ marginBottom: 24 }}>
        Get Another Quote
      </Button>

      <Row gutter={[24, 24]}>
        {/* Summary Cards */}
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="Monthly Premium"
              value={result.monthlyPremiumUsd}
              precision={2}
              prefix={<DollarOutlined />}
              suffix="/month"
              valueStyle={{ color: '#1677ff', fontSize: 32 }}
            />
            <Text type="secondary">Annual: ${result.annualPremiumUsd.toFixed(2)}</Text>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="Risk Score"
              value={result.riskScore}
              precision={1}
              suffix="/100"
              valueStyle={{ color: TIER_COLOR[tier] === 'green' ? '#52c41a' : TIER_COLOR[tier] === 'gold' ? '#faad14' : TIER_COLOR[tier] === 'orange' ? '#fa8c16' : '#f5222d' }}
            />
            <Tag color={TIER_COLOR[tier]} style={{ marginTop: 8, fontSize: 14 }}>
              <SafetyCertificateOutlined /> {TIER_LABEL[tier]}
            </Tag>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="Coverage Type"
              value={result.coverageType}
              valueStyle={{ fontSize: 24 }}
            />
            <Text type="secondary">
              Quote ID: {result.quoteId.substring(0, 8)}...
            </Text>
          </Card>
        </Col>

        {/* AI Explanation */}
        {result.aiExplanation && (
          <Col xs={24}>
            <Card
              title={<Space><InfoCircleOutlined /><span>AI Risk Explanation</span></Space>}
              style={{ background: '#f0f5ff', borderColor: '#adc6ff' }}
            >
              <Paragraph style={{ fontSize: 15, margin: 0 }}>
                {result.aiExplanation}
              </Paragraph>
            </Card>
          </Col>
        )}

        {/* Risk Breakdown */}
        <Col xs={24}>
          {result.appliedFactors.length > 0 ? (
            <RiskBreakdownCard factors={result.appliedFactors} />
          ) : (
            <Alert
              type="success"
              icon={<SafetyCertificateOutlined />}
              message="Excellent Risk Profile"
              description="No significant risk factors were identified. You qualify for standard rates."
              showIcon
            />
          )}
        </Col>

        {/* Disclaimer */}
        <Col xs={24}>
          <Card size="small" style={{ background: '#fafafa' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              <strong>Disclaimer:</strong> This is an estimate based on the information provided.
              Actual premiums may vary based on additional underwriting factors, state regulations,
              and verification of provided information. Quote ID: {result.quoteId}
            </Text>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default QuoteResult;
