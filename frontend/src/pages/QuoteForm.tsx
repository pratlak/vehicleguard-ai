import React, { useState } from 'react';
import {
  Card, Form, InputNumber, Select, Input, Button, Row, Col, Typography, Alert, Divider, Space
} from 'antd';
import { CarOutlined, UserOutlined, EnvironmentOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { assessRisk } from '../api/riskApi';
import { AssessRequest, AssessResponse } from '../types';

const { Title, Text } = Typography;
const { Option } = Select;

interface QuoteFormProps {
  onResult: (result: AssessResponse) => void;
}

const QuoteForm: React.FC<QuoteFormProps> = ({ onResult }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onFinish = async (values: AssessRequest) => {
    setLoading(true);
    setError(null);
    try {
      const result = await assessRisk(values);
      onResult(result);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to assess risk. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <Title level={2}>Get Your Insurance Quote</Title>
        <Text type="secondary">
          Fill in your details below to receive an AI-powered risk assessment and premium estimate.
        </Text>
      </div>

      {error && (
        <Alert type="error" message={error} showIcon closable onClose={() => setError(null)}
               style={{ marginBottom: 24 }} />
      )}

      <Form form={form} layout="vertical" onFinish={onFinish} size="large">
        <Row gutter={24}>
          {/* Driver Information */}
          <Col xs={24} md={12}>
            <Card
              title={<Space><UserOutlined /><span>Driver Information</span></Space>}
              style={{ marginBottom: 24 }}
            >
              <Form.Item name="driverAge" label="Driver Age"
                rules={[{ required: true, message: 'Please enter driver age' },
                        { type: 'number', min: 16, max: 120 }]}>
                <InputNumber min={16} max={120} style={{ width: '100%' }} placeholder="e.g. 28" />
              </Form.Item>

              <Form.Item name="licenseYears" label="Years Licensed"
                rules={[{ required: true, message: 'Please enter years licensed' },
                        { type: 'number', min: 0 }]}>
                <InputNumber min={0} style={{ width: '100%' }} placeholder="e.g. 5" />
              </Form.Item>

              <Form.Item name="violationsLast5Yr" label="Traffic Violations (last 5 years)"
                rules={[{ required: true, message: 'Please enter violations count' },
                        { type: 'number', min: 0 }]}>
                <InputNumber min={0} style={{ width: '100%' }} placeholder="e.g. 0" />
              </Form.Item>

              <Form.Item name="accidentsLast5Yr" label="At-Fault Accidents (last 5 years)"
                rules={[{ required: true, message: 'Please enter accidents count' },
                        { type: 'number', min: 0 }]}>
                <InputNumber min={0} style={{ width: '100%' }} placeholder="e.g. 0" />
              </Form.Item>
            </Card>
          </Col>

          {/* Vehicle Information */}
          <Col xs={24} md={12}>
            <Card
              title={<Space><CarOutlined /><span>Vehicle Information</span></Space>}
              style={{ marginBottom: 24 }}
            >
              <Form.Item name="vehicleMake" label="Vehicle Make">
                <Input placeholder="e.g. Ford, Toyota, BMW" />
              </Form.Item>

              <Form.Item name="vehicleModel" label="Vehicle Model">
                <Input placeholder="e.g. Mustang, Camry, X5" />
              </Form.Item>

              <Form.Item name="vehicleYear" label="Vehicle Year"
                rules={[{ type: 'number', min: 1900, max: new Date().getFullYear() + 1 }]}>
                <InputNumber min={1900} max={new Date().getFullYear() + 1}
                             style={{ width: '100%' }} placeholder="e.g. 2021" />
              </Form.Item>
            </Card>

            <Card
              title={<Space><EnvironmentOutlined /><span>Location & Coverage</span></Space>}
              style={{ marginBottom: 24 }}
            >
              <Form.Item name="zipCode" label="ZIP Code"
                rules={[{ required: true, message: 'Please enter ZIP code' },
                        { pattern: /^\d{5}$/, message: 'ZIP code must be 5 digits' }]}>
                <Input placeholder="e.g. 94105" maxLength={5} />
              </Form.Item>

              <Form.Item name="stateCode" label="State">
                <Select placeholder="Select state" allowClear showSearch>
                  {['AL','AK','AZ','AR','CA','CO','CT','DE','FL','GA','HI','ID','IL','IN',
                    'IA','KS','KY','LA','ME','MD','MA','MI','MN','MS','MO','MT','NE','NV',
                    'NH','NJ','NM','NY','NC','ND','OH','OK','OR','PA','RI','SC','SD','TN',
                    'TX','UT','VT','VA','WA','WV','WI','WY'].map(s => (
                    <Option key={s} value={s}>{s}</Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item name="coverageType" label="Coverage Type"
                rules={[{ required: true, message: 'Please select coverage type' }]}>
                <Select placeholder="Select coverage">
                  <Option value="LIABILITY">Liability Only</Option>
                  <Option value="COLLISION">Collision</Option>
                  <Option value="COMPREHENSIVE">Comprehensive</Option>
                  <Option value="FULL">Full Coverage</Option>
                </Select>
              </Form.Item>
            </Card>
          </Col>
        </Row>

        <Divider />

        <div style={{ textAlign: 'center' }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            size="large"
            icon={<SafetyCertificateOutlined />}
            style={{ minWidth: 200 }}
          >
            {loading ? 'Calculating...' : 'Get My Quote'}
          </Button>
        </div>
      </Form>
    </div>
  );
};

export default QuoteForm;
